package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryEntity
import com.example.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HumanizerUiState {
    object Idle : HumanizerUiState
    data class Processing(val currentChunk: Int, val totalChunks: Int, val progressPercent: Float) : HumanizerUiState
    object Success : HumanizerUiState
    data class Error(val message: String) : HumanizerUiState
}

class HumanizerViewModel(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {

    // Input States
    val inputText = MutableStateFlow("")
    val selectedMode = MutableStateFlow("academic_advanced") // Default Turnitin bypass mode
    
    // Ultimate Turnitin Bypass Properties
    val eraseFingerprints = MutableStateFlow(true)
    val burstinessLevel = MutableStateFlow(1) // 0 = Low, 1 = Medium, 2 = Extreme
    val preserveStructure = MutableStateFlow(true)
    
    // UI processing overall status
    private val _uiState = MutableStateFlow<HumanizerUiState>(HumanizerUiState.Idle)
    val uiState: StateFlow<HumanizerUiState> = _uiState.asStateFlow()

    // Output States
    private val _originalTextOutput = MutableStateFlow("")
    val originalTextOutput: StateFlow<String> = _originalTextOutput.asStateFlow()

    private val _humanizedTextOutput = MutableStateFlow("")
    val humanizedTextOutput: StateFlow<String> = _humanizedTextOutput.asStateFlow()

    private val _estimatedScore = MutableStateFlow(0)
    val estimatedScore: StateFlow<Int> = _estimatedScore.asStateFlow()

    // History Database Flow
    val historyList: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onInputTextChange(text: String) {
        inputText.value = text
    }

    fun onModeChange(mode: String) {
        selectedMode.value = mode
    }

    fun onEraseFingerprintsChange(value: Boolean) {
        eraseFingerprints.value = value
    }

    fun onBurstinessLevelChange(level: Int) {
        burstinessLevel.value = level
    }

    fun onPreserveStructureChange(value: Boolean) {
        preserveStructure.value = value
    }

    fun clearOutputs() {
        _originalTextOutput.value = ""
        _humanizedTextOutput.value = ""
        _estimatedScore.value = 0
        _uiState.value = HumanizerUiState.Idle
    }

    fun humanizeText() {
        val originalText = inputText.value
        val mode = selectedMode.value
        
        if (originalText.isBlank()) {
            _uiState.value = HumanizerUiState.Error("Please enter some text to humanize.")
            return
        }

        viewModelScope.launch {
            _uiState.value = HumanizerUiState.Processing(0, 0, 0f)
            
            // Split into 250-word chunks (perfect size for retaining full sentence structures and context)
            val chunks = repository.splitTextIntoChunks(originalText, maxWords = 250)
            val totalChunks = chunks.size
            val humanizedChunks = mutableListOf<String>()

            var hasError = false
            var errorMessage = ""

            for (index in chunks.indices) {
                val currentChunkNum = index + 1
                val progress = index.toFloat() / totalChunks.toFloat()
                _uiState.value = HumanizerUiState.Processing(currentChunkNum, totalChunks, progress)

                // Perform direct REST API call to Gemini model with advanced features
                val result = repository.humanizeChunk(
                    chunk = chunks[index],
                    mode = mode,
                    eraseFingerprints = eraseFingerprints.value,
                    burstinessLevel = burstinessLevel.value,
                    preserveStructure = preserveStructure.value
                )
                
                if (result == "API_KEY_MISSING_ERROR") {
                    hasError = true
                    errorMessage = "Gemini API Key is missing. Please configure your GEMINI_API_KEY securely in the AI Studio Secrets panel."
                    break
                } else if (result.startsWith("Error:")) {
                    hasError = true
                    errorMessage = result
                    break
                } else {
                    humanizedChunks.add(result)
                }
            }

            if (hasError) {
                _uiState.value = HumanizerUiState.Error(errorMessage)
            } else {
                val finalHumanizedText = humanizedChunks.joinToString("\n\n")
                _originalTextOutput.value = originalText
                _humanizedTextOutput.value = finalHumanizedText

                // Calculate human score metrics locally
                val computedScore = repository.calculateLocalMetrics(originalText, finalHumanizedText)
                _estimatedScore.value = computedScore

                // Generate rapid 3-5 word Title for History
                val titlePreview = originalText.trim().split(Regex("\\s+")).take(4).joinToString(" ") + "..."

                // Save to historical Room database automatically
                val countOrig = originalText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                val countHum = finalHumanizedText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                
                val historyItem = HistoryEntity(
                    title = titlePreview,
                    originalText = originalText,
                    humanizedText = finalHumanizedText,
                    wordCountOriginal = countOrig,
                    wordCountHumanized = countHum,
                    score = computedScore,
                    mode = mode
                )
                repository.insertHistory(historyItem)
                
                _uiState.value = HumanizerUiState.Success
            }
        }
    }

    fun deleteHistoryItem(item: HistoryEntity) {
        viewModelScope.launch {
            repository.deleteHistory(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Factory Class for creating ViewModel with required dependencies
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HumanizerViewModel::class.java)) {
                val database = AppDatabase.getDatabase(application)
                val repository = Repository(database.historyDao())
                return HumanizerViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
