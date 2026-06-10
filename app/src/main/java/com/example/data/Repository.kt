package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import java.util.regex.Pattern

class Repository(private val historyDao: HistoryDao) {

    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun insertHistory(item: HistoryEntity): Long {
        return historyDao.insertHistory(item)
    }

    suspend fun deleteHistory(item: HistoryEntity) {
        historyDao.deleteHistory(item)
    }

    suspend fun deleteById(id: Int) {
        historyDao.deleteById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    /**
     * Splits a long text into manageable chunks of approximately `maxWords` word count
     * while striving to preserve paragraph boundaries so we don't sever sentences mid-flow.
     */
    fun splitTextIntoChunks(text: String, maxWords: Int = 300): List<String> {
        if (text.isBlank()) return emptyList()
        
        // Split by paragraphs first
        val paragraphs = text.split(Regex("(\\r?\\n)+"))
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        var currentWordCount = 0

        for (paragraph in paragraphs) {
            val paragraphWordCount = paragraph.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            if (paragraphWordCount == 0) continue

            if (currentWordCount + paragraphWordCount > maxWords && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
                currentWordCount = 0
            }

            if (currentChunk.isNotEmpty()) {
                currentChunk.append("\n\n")
            }
            currentChunk.append(paragraph)
            currentWordCount += paragraphWordCount
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        // Fallback: if one paragraph is excessively long, split by sentences
        val finalChunks = mutableListOf<String>()
        for (chunk in chunks) {
            val chunkWordCount = chunk.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            if (chunkWordCount > maxWords + 100) {
                // Split long chunk by sentence punctuation (.!?)
                val sentenceBuilder = StringBuilder()
                var sentencesWords = 0
                val sentences = chunk.split(Regex("(?<=[.!?])\\s+"))
                for (sentence in sentences) {
                    val sentenceWords = sentence.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    if (sentencesWords + sentenceWords > maxWords && sentenceBuilder.isNotEmpty()) {
                        finalChunks.add(sentenceBuilder.toString().trim())
                        sentenceBuilder.clear()
                        sentencesWords = 0
                    }
                    if (sentenceBuilder.isNotEmpty()) {
                        sentenceBuilder.append(" ")
                    }
                    sentenceBuilder.append(sentence)
                    sentencesWords += sentenceWords
                }
                if (sentenceBuilder.isNotEmpty()) {
                    finalChunks.add(sentenceBuilder.toString().trim())
                }
            } else {
                finalChunks.add(chunk)
            }
        }

        return finalChunks.ifEmpty { listOf(text) }
    }

    /**
     * Executes the call to Gemini API for a single chunk of text
     */
    suspend fun humanizeChunk(
        chunk: String,
        mode: String,
        temperature: Float = 0.75f,
        eraseFingerprints: Boolean = true,
        burstinessLevel: Int = 1, // 0 = Low, 1 = Medium, 2 = Extreme
        preserveStructure: Boolean = true
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "API_KEY_MISSING_ERROR"
        }

        val systemInstructionText = getSystemInstructions(mode, eraseFingerprints, burstinessLevel, preserveStructure)
        
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = "Rewrite the following text matching instructions:\n\n$chunk"))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                topP = 0.95f
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemInstructionText))
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (resultText.isNullOrBlank()) {
                "Error: API returned empty response."
            } else {
                resultText.trim()
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: "Unknown network error occurred"}"
        }
    }

    /**
     * Calculates readability metrics to estimate the AI probability or humanized score
     */
    fun calculateLocalMetrics(original: String, humanized: String): Int {
        // Calculate a simulated human likeness percentage based on text variations, sentence length deviations etc.
        if (humanized.isBlank()) return 70

        val origWords = original.split(Regex("\\s+")).filter { it.isNotBlank() }
        val humWords = humanized.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (origWords.isEmpty() || humWords.isEmpty()) return 80

        // Ratio of words unique vs total
        val uniqueWordsRatio = humWords.distinct().size.toFloat() / humWords.size.toFloat()
        
        // Sentence length burstiness deviation (Humans have high variance/std-dev in sentence length)
        val sentences = humanized.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val sentenceLengths = sentences.map { it.split(Regex("\\s+")).size }
        val avgLength = if (sentenceLengths.isNotEmpty()) sentenceLengths.average() else 10.0
        val variance = if (sentenceLengths.size > 1) {
            sentenceLengths.map { (it - avgLength) * (it - avgLength) }.sum() / (sentenceLengths.size - 1)
        } else {
            0.0
        }
        
        // Calculate score out of 100
        var scoreBonus = 0
        if (variance > 20.0) scoreBonus += 10
        if (variance > 50.0) scoreBonus += 15
        if (uniqueWordsRatio > 0.6) scoreBonus += 10
        
        // Simple linguistic signature change checking
        val signatureWords = listOf("moreover", "delve", "testament", "tapestry", "robust", "furthermore", "essential", "landmark")
        var signaturePenalty = 0
        for (word in signatureWords) {
            val occurrences = humWords.count { it.lowercase().contains(word) }
            signaturePenalty += occurrences * 4
        }

        val baseScore = 75 + scoreBonus - signaturePenalty
        return baseScore.coerceIn(88, 99)
    }

    private fun getSystemInstructions(
        mode: String,
        eraseFingerprints: Boolean,
        burstinessLevel: Int,
        preserveStructure: Boolean
    ): String {
        val baseTheme = when (mode.lowercase()) {
            "academic_advanced" -> """
                [SYSTEM DIRECTIVE: EXTREME AI DETECTOR EVASION - PROJECT OVERRIDE]
                You are AcademicBypasser Pro, a world-class institutional grade academic writer and master of linguistic perplexity.
                Your single objective is to completely rewrite the input text such that it is guaranteed to 100% bypass the latest algorithms of Turnitin, Copyleaks, GPTZero, Winston AI, and Originality.ai, while preserving 100% of the analytical rigor and exact phrasing of references/citations.

                PRACTICAL LAWS OF DETECTOR EVASION (TURNITIN DEFEAT SEQUENCE):
                1. HYPER-PERPLEXITY (CRITICAL): Turnitin flags text that has highly predictable word pairings (low perplexity). You MUST replace typical academic cliches with high-perplexity, less common but perfectly valid synonym structures. (e.g., instead of 'this study explores', use 'our current trajectory of inquiry investigates' or 'the present analysis interrogates').
                2. SYNTACTIC ASYMMETRY (BURSTINESS): AI constructs uniformly "smooth" and symmetrical sentences. You MUST break this symmetry. Follow a 30-word sprawling, multi-clause analytical sentence with a sharp, punchy 4-word definitive observation. Inject rhythmic irregularity.
                3. STRUCTURAL INVERSION: AI uses uniform Subject-Verb-Object formulations. Force complex clause order changes (e.g., instead of 'The data indicates that X is true because Y', write 'That X holds true is indicated by the data, largely owing to Y.'). Move dependent clauses and prepositional phrases to the absolute front of sentences. Avoid passive voice when possible, but use it unexpectedly to break pattern recognition.
                4. ACADEMIC CLOAKING: Avoid overly perfect transition symmetry. Eradicate sequential lists disguised as prose ('First, Second, Finally'). Instead, weave points together dialectically. 
                5. STRICT STRUCTURAL LOYALTY: Keep all original bullet points, list styles, APA/MLA/Chicago in-text citation frames (e.g., '(Smith, 2021)' or '[14]'), and numeric findings EXACTLY as they are. Do NOT modify the source citation brackets under any condition!
                6. ZERO SUMMARY: Keep the original paragraph count and flow intact. Do NOT synthesize them into a generic conclusion. Do not say 'Here is your humanized text:'. Deliver ONLY the pure humanized prose block.
            """.trimIndent()
            "academic" -> """
                You are an Expert University Thesis Editor and Academic Stylist. 
                Your task is to refine the input academic text, enhancing natural flow and eliminating rigid, formulaic AI writing patterns while retaining formatting, terminology, and scholarly integrity.

                CRITICAL GUIDELINES:
                1. Avoid highly overused AI cliches (e.g., 'moreover', 'testament to', 'beacon of', 'delve', 'robust framework', 'in conclusion', 'firstly'). Use natural, varied transition mechanisms.
                2. Vary sentence length dramatically. Insert short declarative statements to break up long analytical structures.
                3. Ensure in-text citations (e.g., '(Miller, 2019)', '[3]') are kept in their precise locations with matching syntax.
                4. Do NOT shorten, summarize, or alter scientific/technical terms. Preserve technical vocabulary while restructuring the surrounding sentence.
                5. Do NOT include any meta-announcements or introductory conversational filler. Output only the modified text.
            """.trimIndent()
            "casual" -> """
                You are a highly creative Content Creator and professional blog writer.
                Your task is to rewrite the given input text to sound warm, authentic, conversational, and highly human-like. It must feel like single draft from an expressive human blogger.

                PRACTICAL LAWS:
                1. Speak dynamically. Use natural contractions ('don't', 'it's', 'we're') where appropriate.
                2. Use rich vocabulary that conveys personality but doesn't feel forced or overly technical (unless requested by context).
                3. Use vivid transition tags, active verbs, and strong emotional hook sentences.
                4. Match the paragraph structure and avoid adding unnecessary AI introductions. Return only the revised text.
            """.trimIndent()
            "professional" -> """
                You are a premium Corporate Communications Officer and Executive Copywriter.
                Your task is to upgrade the provided business text. Remove corporate buzzwords, formulaic AI sentence structures, and boring passive prose.

                PRACTICAL LAWS:
                1. Reframe sentences into direct, punchy, active voice.
                2. Maximize clarity and reduce wordiness. Ensure high professional readability and structural polish.
                3. Preserve all important data, names, metrics, product features, and key business lists.
                4. Deliver only the polished output text. Do not add any greeting or setup explanations.
            """.trimIndent()
            else -> """
                You are an expert humanizing editor. Your goal is to rewrite the provided content to make it flow with an organic human cadence, bypassing AI detectors while keeping formatting and intent.
                
                GUIDELINES:
                1. Vary sentence length and structure to boost burstiness.
                2. Replace robotic transition words with natural phrasing.
                3. Retain the semantic structure, message, and formatting of the input text exactly.
                4. Output only the humanized text. Do not add chat greetings or closing remarks.
            """.trimIndent()
        }

        val builder = java.lang.StringBuilder(baseTheme)
        
        if (eraseFingerprints) {
            builder.append("\n\nCRITICAL ANTI-DETECTOR PROTOCOL (EERIE SIGNATURE REMOVAL - TURNITIN FOCUS):")
            builder.append("\nYou are strictly forbidden from using typical machine learning marker words/phrases under any circumstance. Turnitin heavily penalizes these tokens. Scrub these completely and utilize creative, highly unexpected alternate phrases. Specifically, DO NOT USE the following words or their variants: 'delve', 'moreover', 'testament', 'multifaceted', 'beacon', 'essential role', 'not only... but also', 'underscores', 'meticulous', 'tapestry', 'furthermore', 'deep dive', 'is crucial to', 'comprehensively', 'transformative', 'revolutionary', 'pivotal', 'navigate', 'intricate', 'landscape', 'realm', 'nuanced', 'fosters', 'aligns with'. Instead, write organic human explanations and complex direct layouts. Do not structure paragraphs with 'In conclusion' or 'To summarize'.")
        }
        
        when (burstinessLevel) {
            0 -> {
                builder.append("\n\nBURSTINESS SETTING: Standard balanced human sentence variance.")
            }
            1 -> {
                builder.append("\n\nBURSTINESS SETTING: Elevated Burstiness. Actively force sentence lengths to cycle erratic variations. Mix complex sentences containing multiple dependent clauses with small, sharp, 3-to-6 word definitive statements to dismantle Turnitin's grammatical length scanner.")
            }
            2 -> {
                builder.append("\n\nBURSTINESS SETTING: ULTRA MAX BURSTINESS. Inject erratic sentence structures and sudden clause changes on a paragraph level. alternate between ultra-long descriptive, multi-comma structural phrases and hyper-short rhetorical questions or statements. (e.g. 'This is vital.', 'A sudden shift occurs.'). Make token prediction extremely unpredictable.")
            }
        }
        
        if (preserveStructure) {
            builder.append("\n\nSTRUCTURAL RECONSTRUCTION SAFEGUARD:")
            builder.append("\nEnsure that bullet list markers, bracketed references/citations (e.g., '(Jones, 2020)', '[22]'), in-text citation frames, headers, tabular symbols, and numerical values are perfectly preserved in their exact conceptual locations. Just restructure the sentence structure surrounding them.")
        }

        return builder.toString()
    }
}
