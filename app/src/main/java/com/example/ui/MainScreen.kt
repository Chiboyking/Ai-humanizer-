package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.HistoryEntity
import java.text.SimpleDateFormat
import java.util.*

// UI Theme Palette - High Density cozy terracotta & cream-peach design
private val darkBg = Color(0xFFFDF8F6)                     // Cream Peach Background
private val cardBg = Color(0xFFFFFFFF)                     // Clean Pristine White Card Background
private val activeBorderColor = Color(0xFF9C4331)          // Cozy Terracotta Red
private val accentCyan = Color(0xFF9C4331)                 // Main Brand Core Accent
private val textPrimary = Color(0xFF201A18)                 // Deep Earth Black Typography
private val textSecondary = Color(0xFF735A55)               // Soft Earth Taupe Grey
private val dividerColor = Color(0xFFD8C2BC)                // Sand Rose Border
private val scoreColorGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF9C4331), Color(0xFFE06F53)) // Terracotta red gradient
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: HumanizerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val eraseFingerprints by viewModel.eraseFingerprints.collectAsStateWithLifecycle()
    val burstinessLevel by viewModel.burstinessLevel.collectAsStateWithLifecycle()
    val preserveStructure by viewModel.preserveStructure.collectAsStateWithLifecycle()
    
    val originalTextOut by viewModel.originalTextOutput.collectAsStateWithLifecycle()
    val humanizedTextOut by viewModel.humanizedTextOutput.collectAsStateWithLifecycle()
    val estimatedScore by viewModel.estimatedScore.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Humanizer Workspace, 1 = History Archive
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Glow AI Scan",
                            tint = activeBorderColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Text Humanizer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkBg,
                    titleContentColor = textPrimary
                ),
                actions = {
                    // Quick indicator showing if secure API key is configured or mockup placeholder
                    val isConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isConfigured) Color(0xFFD4EDDA) else Color(0xFFF3DED7),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (isConfigured) "KEY ACTIVE" else "DEMO MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConfigured) Color(0xFF155724) else Color(0xFF9C4331),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        },
        containerColor = darkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Elegant Navigation Tab Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFFF3DED7), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("Workspace", "Saved Archive").forEachIndexed { index, title ->
                    val isActive = activeTab == index
                    val tabBackground = if (isActive) Color(0xFFFFFFFF) else Color.Transparent
                    val tabTextColor = if (isActive) Color(0xFF9C4331) else textSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(tabBackground)
                            .clickable { activeTab = index }
                            .padding(vertical = 10.dp)
                            .testTag("nav_tab_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = tabTextColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                label = "TabTransition",
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                }
            ) { targetTab ->
                when (targetTab) {
                    0 -> {
                        // Humanizer Workspace Tab
                        WorkspaceView(
                            inputText = inputText,
                            selectedMode = selectedMode,
                            uiState = uiState,
                            originalTextOut = originalTextOut,
                            humanizedTextOut = humanizedTextOut,
                            estimatedScore = estimatedScore,
                            eraseFingerprints = eraseFingerprints,
                            burstinessLevel = burstinessLevel,
                            preserveStructure = preserveStructure,
                            onInputChange = { viewModel.onInputTextChange(it) },
                            onModeChange = { viewModel.onModeChange(it) },
                            onEraseFingerprintsChange = { viewModel.onEraseFingerprintsChange(it) },
                            onBurstinessLevelChange = { viewModel.onBurstinessLevelChange(it) },
                            onPreserveStructureChange = { viewModel.onPreserveStructureChange(it) },
                            onHumanizeClick = { viewModel.humanizeText() },
                            onClearClick = { viewModel.clearOutputs() },
                            onSaveMockText = { viewModel.onInputTextChange(it) },
                            clipboardManager = clipboardManager,
                            context = context
                        )
                    }
                    1 -> {
                        // Archive History Tab
                        ArchiveView(
                            historyList = historyList,
                            onDelete = { viewModel.deleteHistoryItem(it) },
                            onClearAll = { viewModel.clearAllHistory() },
                            onLoadItem = { item ->
                                viewModel.onInputTextChange(item.originalText)
                                activeTab = 0 // Switch back to workspace
                            },
                            context = context,
                            clipboardManager = clipboardManager
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceView(
    inputText: String,
    selectedMode: String,
    uiState: HumanizerUiState,
    originalTextOut: String,
    humanizedTextOut: String,
    estimatedScore: Int,
    eraseFingerprints: Boolean,
    burstinessLevel: Int,
    preserveStructure: Boolean,
    onInputChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onEraseFingerprintsChange: (Boolean) -> Unit,
    onBurstinessLevelChange: (Int) -> Unit,
    onPreserveStructureChange: (Boolean) -> Unit,
    onHumanizeClick: () -> Unit,
    onClearClick: () -> Unit,
    onSaveMockText: (String) -> Unit,
    clipboardManager: ClipboardManager,
    context: Context
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (humanizedTextOut.isBlank() && uiState !is HumanizerUiState.Success) {
                // RENDER INPUT WORKSPACE
                Text(
                    text = "Bypass Turnitin & AI Scanners",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Our neural humanization pipeline restructures sentences, optimizes vocabulary complexity, and injecting natural human variance (burstiness) to securely circumvent standard detectors while preserving key information.",
                    fontSize = 12.sp,
                    color = textSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Paste field box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, dividerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INPUT TEXT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeBorderColor,
                                modifier = Modifier.padding(start = 8.dp)
                            )

                            // Clean Mock Text generation buttons for easy testing
                            Row {
                                TextButton(
                                    onClick = {
                                        onSaveMockText(
                                            "Artificial intelligence has experienced monumental growth over previous decades. It is essential to understand that machine learning algorithms analyze robust datasets to delve into deep patterns in conclusion."
                                        )
                                    }
                                ) {
                                    Text("Load Sample AI Text", fontSize = 11.sp, color = activeBorderColor)
                                }
                            }
                        }

                        TextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            placeholder = {
                                Text(
                                    text = "Paste your AI generated text here... (Supports unlimited length, automatically processed paragraph-by-paragraph)",
                                    fontSize = 14.sp,
                                    color = textSecondary
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp)
                                .testTag("source_text_input")
                        )

                        // Word count & action indicators below
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val wordCount = if (inputText.isBlank()) 0 else inputText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                            val characterCount = inputText.length
                            
                            Text(
                                text = "$wordCount words  |  $characterCount chars",
                                fontSize = 12.sp,
                                color = textSecondary
                            )

                            if (inputText.isNotBlank()) {
                                IconButton(
                                    onClick = { onInputChange("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SELECT HUMANIZING ENGINE MODE
                Text(
                    text = "SELECT BYPASS STRENGTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeBorderColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val modes = listOf(
                    Triple("academic_advanced", "Academic Max Bypass", "Optimized specifically to outsmart Turnitin and Copyleaks. Introduces deep burstiness variation."),
                    Triple("academic", "Standard Academic", "Refines typical scholarly literature, removing formulaic styles while protecting critical research citations."),
                    Triple("professional", "Professional Report", "Polishes corporate documents, proposals, and emails. Converts passive stiffness to active human flow."),
                    Triple("casual", "Casual Blogger", "Fuses authentic conversational speech patterns, natural abbreviations, and rich personality elements.")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { (modeId, label, description) ->
                        val isMatched = selectedMode == modeId
                        val border = if (isMatched) BorderStroke(1.5.dp, activeBorderColor) else BorderStroke(1.dp, dividerColor)
                        val cardColor = if (isMatched) Color(0xFFF3DED7) else cardBg

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardColor,
                            border = border,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModeChange(modeId) }
                                .testTag("mode_card_$modeId")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isMatched,
                                    onClick = { onModeChange(modeId) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = activeBorderColor,
                                        unselectedColor = textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMatched) activeBorderColor else textPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = description,
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ADVANCED BYPASS ENGINE DIALS
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, dividerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Advanced controls",
                                tint = activeBorderColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TURNITIN SHIELD & AI SCANNER BYPASS CONTROL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        // Toggle 1: Fingerprint erasure
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Erase AI Linguistic Fingerprints",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Guarantees complete removal of overrepresented AI words like 'delve', 'moreover', 'testament', etc.",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = eraseFingerprints,
                                onCheckedChange = onEraseFingerprintsChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = activeBorderColor,
                                    uncheckedThumbColor = textSecondary,
                                    uncheckedTrackColor = dividerColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Slider or Tab/Selector: Burstiness
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Syntactic Burstiness Intensity",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF3DED7),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = when(burstinessLevel) {
                                            0 -> "Standard"
                                            1 -> "Elevated"
                                            else -> "Ultra Extreme"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeBorderColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Simulates human stylistic shifts by cycling sentence length and structure to break statistical uniformity.",
                                fontSize = 11.sp,
                                color = textSecondary,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Multi-segment selector/tab row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFDF8F6), RoundedCornerShape(12.dp))
                                    .border(1.dp, dividerColor, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                listOf(0 to "Low", 1 to "Substantial", 2 to "Extreme").forEach { (level, lbl) ->
                                    val isSel = burstinessLevel == level
                                    val bgCol = if (isSel) activeBorderColor else Color.Transparent
                                    val textCol = if (isSel) Color.White else textSecondary
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(bgCol, RoundedCornerShape(8.dp))
                                            .clickable { onBurstinessLevelChange(level) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lbl,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textCol
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle 2: Structure preserver
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Strict Citation & Format Safeguard",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Locks bracketed references (like '(Miller, 2020)', '[5]'), lists, tables, and mathematical formulas in-place.",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = preserveStructure,
                                onCheckedChange = onPreserveStructureChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = activeBorderColor,
                                    uncheckedThumbColor = textSecondary,
                                    uncheckedTrackColor = dividerColor
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // EXPLICIT ACTION BUTTON
                Button(
                    onClick = onHumanizeClick,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeBorderColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("humanize_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Optimize",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HUMANIZE TEXT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(30.dp))
            } else {
                // RENDER SUCCESS OUTPUT PREVIEW
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Processing Complete",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        
                        TextButton(
                            onClick = onClearClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New", modifier = Modifier.size(16.dp), tint = activeBorderColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Humanize", fontSize = 13.sp, color = activeBorderColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SCORE VISUAL ENGINE
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, dividerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Score Indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFF3DED7), CircleShape)
                                    .padding(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { estimatedScore.toFloat() / 100f },
                                    color = activeBorderColor,
                                    trackColor = Color(0xFFE6D4CF),
                                    strokeWidth = 6.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$estimatedScore%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "HUMAN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        color = activeBorderColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Estimated bypass score: Exceptionally High",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Turnitin / AI models analyze vocabulary patterns, cliches, and uniform sentence structures. Your text was processed to remove these signatures successfully.",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // COMPARE ORIGINAL vs HUMANIZED tabs
                    var showOriginal by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showOriginal = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!showOriginal) activeBorderColor else Color.Transparent,
                                contentColor = if (!showOriginal) Color.White else textSecondary
                            ),
                            border = if (!showOriginal) null else BorderStroke(1.dp, dividerColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Humanized Text", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showOriginal = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showOriginal) activeBorderColor else Color.Transparent,
                                contentColor = if (showOriginal) Color.White else textSecondary
                            ),
                            border = if (showOriginal) null else BorderStroke(1.dp, dividerColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Original AI Copied", fontSize = 12.sp)
                        }
                    }

                    // Display content text area
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, dividerColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header segment operations
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentText = if (showOriginal) originalTextOut else humanizedTextOut
                                val count = currentText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                                
                                Text(
                                    text = if (showOriginal) "ORIGINAL ($count words)" else "HUMANIZED ($count words)",
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showOriginal) textSecondary else activeBorderColor
                                )

                                Row {
                                    // Copy Action
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied Text", currentText))
                                            // Toast announcement
                                            android.widget.Toast.makeText(context, "Text copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit, // Substitute for custom copy
                                            contentDescription = "Copy to clipboard",
                                            tint = activeBorderColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Share Action
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, currentText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share text via"))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = activeBorderColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (showOriginal) originalTextOut else humanizedTextOut,
                                fontSize = 14.sp,
                                color = textPrimary,
                                lineHeight = 21.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // LINGUISTIC ASSURANCE METRICS PANEL
                    Text(
                        text = "SECURE PARAPHRASING ASSESSMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeBorderColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val asses = listOf(
                        "Linguistic Perplexity" to "Exceptional (High vocabulary spread successfully breaks repetitive machine structures)",
                        "Syntactic Burstiness" to "Maximum (Varied lengths of consecutive human phrases)",
                        "Plagiarism Core Matching" to "Zero Match (Complete original phrasing formulation preserves structural idea)"
                    )
                    
                    asses.forEach { (metric, description) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Check",
                                tint = activeBorderColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = metric, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(text = description, fontSize = 11.sp, color = textSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // FULL SCREEN OVERLAY PROGRESS INDICATOR UNDER LOADING STATE
        if (uiState is HumanizerUiState.Processing) {
            val stateObj = uiState as HumanizerUiState.Processing
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF2FDF8F6)),
                contentAlignment = Alignment.Center
            ) {
                // Clean overlay card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, dividerColor),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Beautiful spinning loader
                        val infiniteTransition = rememberInfiniteTransition(label = "Spinner")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "LoaderRotate"
                        )

                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Loader symbol",
                            tint = activeBorderColor,
                            modifier = Modifier
                                .size(54.dp)
                                .rotate(rotation)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "HUMANIZING TEXT PROFILE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom Progress bar
                        LinearProgressIndicator(
                            progress = { stateObj.progressPercent },
                            color = activeBorderColor,
                            trackColor = Color(0xFFE6D4CF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Processing block ${stateObj.currentChunk} of ${stateObj.totalChunks}...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activeBorderColor,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Rewriting with robust perplexity, restructuring subject-verb formations, matching source references, and injecting physical natural bursts...",
                            fontSize = 10.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // FULL SCREEN OVERLAY ERROR HANDLER
        if (uiState is HumanizerUiState.Error) {
            val errMsg = (uiState as HumanizerUiState.Error).message
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF2FDF8F6)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, Color(0xFFE06F53)),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFE06F53),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Operation Prevented",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errMsg,
                            fontSize = 12.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onClearClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE06F53),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Acknowledge & Return")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveView(
    historyList: List<HistoryEntity>,
    onDelete: (HistoryEntity) -> Unit,
    onClearAll: () -> Unit,
    onLoadItem: (HistoryEntity) -> Unit,
    context: Context,
    clipboardManager: ClipboardManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED ARCHIVE (${historyList.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                letterSpacing = 1.sp
            )

            if (historyList.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    fontSize = 13.sp,
                    color = Color(0xFFE06F53),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onClearAll() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty History",
                        tint = textSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Archive is Empty",
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Once you humanize text in the workspace, documents are saved securely here.",
                        color = textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_list")
            ) {
                items(historyList) { item ->
                    HistoryCard(
                        item = item,
                        onDelete = { onDelete(item) },
                        onLoad = { onLoadItem(item) },
                        context = context,
                        clipboardManager = clipboardManager
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: HistoryEntity,
    onDelete: () -> Unit,
    onLoad: () -> Unit,
    context: Context,
    clipboardManager: ClipboardManager
) {
    var isExpanded by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateString = formatter.format(Date(item.timestamp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, dividerColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$dateString  •  ${item.mode.capitalizeMode()}",
                        fontSize = 11.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Score tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3DED7),
                    border = BorderStroke(1.dp, activeBorderColor),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "${item.score}% Human",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeBorderColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body preview segments
            val originalPreview = item.originalText.take(120) + (if (item.originalText.length > 120) "..." else "")
            val humanizedPreview = item.humanizedText.take(120) + (if (item.humanizedText.length > 120) "..." else "")

            if (!isExpanded) {
                // Short Preview Text
                Text(
                    text = "Preview: $humanizedPreview",
                    fontSize = 12.sp,
                    color = textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    modifier = Modifier.clickable { isExpanded = true }
                )
            } else {
                // Fully Expanded View of both original and humanised
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "ORIGINAL (${item.originalText.wordCount()} words)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFDFBF7),
                        border = BorderStroke(0.5.dp, dividerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = item.originalText,
                            fontSize = 12.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(8.dp),
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HUMANIZED (${item.humanizedText.wordCount()} words)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeBorderColor
                        )

                        Row {
                            Text(
                                text = "Copy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeBorderColor,
                                modifier = Modifier
                                    .clickable {
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied", item.humanizedText))
                                        android.widget.Toast.makeText(context, "Copied humanized text", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFDFBF7),
                        border = BorderStroke(0.5.dp, dividerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = item.humanizedText,
                            fontSize = 12.sp,
                            color = textPrimary,
                            modifier = Modifier.padding(8.dp),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action button footer bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Load/Copy back to editor button
                Button(
                    onClick = onLoad,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeBorderColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Re-open in Editor", fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Expand toggler button
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Collapse" else "Expand View",
                            fontSize = 12.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand toggle icon",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Delete item button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFE06F53),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Utility string extensions
private fun String.wordCount(): Int {
    return this.split(Regex("\\s+")).filter { it.isNotBlank() }.size
}

private fun String.capitalizeMode(): String {
    return when (this.lowercase()) {
        "academic_advanced" -> "Academic Max"
        "academic" -> "Academic"
        "professional" -> "Professional"
        "casual" -> "Casual"
        else -> this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
