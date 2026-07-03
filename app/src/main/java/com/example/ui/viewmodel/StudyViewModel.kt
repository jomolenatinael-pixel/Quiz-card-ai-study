package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.StudyRepository
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.api.GeminiStudyResponse
import com.example.data.database.AppDatabase
import com.example.data.database.FlashcardEntity
import com.example.data.database.QuizQuestionEntity
import com.example.data.database.StudySetEntity
import com.example.util.PdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed class Screen {
    object Dashboard : Screen()
    object Create : Screen()
    data class Flashcards(val setId: Long) : Screen()
    data class Quiz(val setId: Long) : Screen()
    object PrivacyPolicy : Screen()
    object AdminPortal : Screen()
}

sealed class GenerationState {
    object Idle : GenerationState()
    object ProcessingPdf : GenerationState()
    object SendingToGemini : GenerationState()
    data class Success(val setId: Long) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository
    private val sharedPrefs = application.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)

    // --- Network Connectivity Tracking ---
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    private val _isOnline = MutableStateFlow(checkInitialNetworkStatus())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private fun checkInitialNetworkStatus(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    private fun registerNetworkCallback() {
        try {
            val request = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: android.net.Network) {
                    _isOnline.value = false
                }
            })
        } catch (e: Exception) {
            _isOnline.value = true
        }
    }

    // --- Active UI Screens ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- State & Settings ---
    private val _darkThemeEnabled = MutableStateFlow(sharedPrefs.getBoolean("dark_theme", true))
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    private val _userApiKey = MutableStateFlow(sharedPrefs.getString("gemini_api_key", "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _developerPhotoBase64 = MutableStateFlow(sharedPrefs.getString("developer_photo_base64", "") ?: "")
    val developerPhotoBase64: StateFlow<String> = _developerPhotoBase64.asStateFlow()

    fun saveDeveloperPhoto(base64: String) {
        _developerPhotoBase64.value = base64
        sharedPrefs.edit().putString("developer_photo_base64", base64).apply()
    }

    fun clearDeveloperPhoto() {
        _developerPhotoBase64.value = ""
        sharedPrefs.edit().remove("developer_photo_base64").apply()
    }

    // --- Generation State ---
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudyRepository(database.studyDao())
        registerNetworkCallback()
    }

    // --- Database Flows ---
    val studySets: StateFlow<List<StudySetEntity>> = repository.allStudySets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentFlashcards = MutableStateFlow<List<FlashcardEntity>>(emptyList())
    val currentFlashcards: StateFlow<List<FlashcardEntity>> = _currentFlashcards.asStateFlow()

    private val _currentQuizQuestions = MutableStateFlow<List<QuizQuestionEntity>>(emptyList())
    val currentQuizQuestions: StateFlow<List<QuizQuestionEntity>> = _currentQuizQuestions.asStateFlow()

    private val _activeStudySet = MutableStateFlow<StudySetEntity?>(null)
    val activeStudySet: StateFlow<StudySetEntity?> = _activeStudySet.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        
        // Handle loading data if entering study modes
        when (screen) {
            is Screen.Flashcards -> {
                loadFlashcards(screen.setId)
            }
            is Screen.Quiz -> {
                loadQuizQuestions(screen.setId)
            }
            else -> {}
        }
    }

    private fun loadFlashcards(setId: Long) {
        viewModelScope.launch {
            repository.getFlashcards(setId).collect { list ->
                _currentFlashcards.value = list
            }
        }
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            _activeStudySet.value = db.studyDao().getStudySetById(setId)
        }
    }

    private fun loadQuizQuestions(setId: Long) {
        viewModelScope.launch {
            repository.getQuizQuestions(setId).collect { list ->
                _currentQuizQuestions.value = list
            }
        }
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            _activeStudySet.value = db.studyDao().getStudySetById(setId)
        }
    }

    suspend fun getFlashcardsForCsv(setId: Long): List<FlashcardEntity> {
        return repository.getFlashcardsList(setId)
    }

    // --- Theme & API Key Configuration ---
    fun toggleTheme() {
        val newVal = !_darkThemeEnabled.value
        _darkThemeEnabled.value = newVal
        sharedPrefs.edit().putBoolean("dark_theme", newVal).apply()
    }

    fun saveApiKey(key: String) {
        _userApiKey.value = key
        sharedPrefs.edit().putString("gemini_api_key", key).apply()
    }

    fun clearApiKey() {
        _userApiKey.value = ""
        sharedPrefs.edit().remove("gemini_api_key").apply()
    }

    fun getEffectiveApiKey(): String {
        val customKey = _userApiKey.value.trim()
        if (customKey.isNotEmpty()) return customKey
        
        // Fallback to BuildConfig if configured
        val configKey = BuildConfig.GEMINI_API_KEY
        if (configKey.isNotEmpty() && !configKey.startsWith("MY_")) {
            return configKey
        }
        return ""
    }

    // --- Leitner Spaced Repetition SRS Engine ---
    fun updateFlashcardBox(flashcard: FlashcardEntity, correct: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextBox = if (correct) {
                minOf(5, flashcard.box + 1)
            } else {
                1 // Return to box 1 on failure
            }
            val updated = flashcard.copy(
                box = nextBox,
                nextReviewTime = FlashcardEntity.getNextReviewTime(nextBox),
                lastStudied = System.currentTimeMillis()
            )
            repository.updateFlashcard(updated)
        }
    }

    // --- Quiz Engine ---
    fun selectQuizAnswer(question: QuizQuestionEntity, selection: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = question.copy(userSelection = selection)
            repository.updateQuizQuestion(updated)
        }
    }

    fun resetQuizAnswers(setId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetQuizAnswers(setId)
        }
    }

    fun deleteStudySet(setId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStudySet(setId)
            if (_currentScreen.value is Screen.Flashcards && (_currentScreen.value as Screen.Flashcards).setId == setId) {
                _currentScreen.value = Screen.Dashboard
            } else if (_currentScreen.value is Screen.Quiz && (_currentScreen.value as Screen.Quiz).setId == setId) {
                _currentScreen.value = Screen.Dashboard
            }
        }
    }

    // --- Gemini Content Generation ---
    fun generateStudySet(
        sourceText: String,
        pdfUri: Uri?,
        pdfFileName: String?
    ) {
        _generationState.value = GenerationState.Idle
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            _generationState.value = GenerationState.Error("Please set a valid Gemini API Key in the settings.")
            return
        }

        viewModelScope.launch {
            try {
                val parts = mutableListOf<Part>()

                if (pdfUri != null) {
                    _generationState.value = GenerationState.ProcessingPdf
                    // Parse PDF pages into Base64 images
                    val pdfParts = withContext(Dispatchers.IO) {
                        PdfParser.extractPdfPagesAsParts(getApplication(), pdfUri, maxPages = 5)
                    }
                    if (pdfParts.isEmpty()) {
                        _generationState.value = GenerationState.Error("Failed to parse PDF document. Ensure it is not corrupted.")
                        return@launch
                    }
                    parts.addAll(pdfParts)
                } else {
                    if (sourceText.trim().isEmpty()) {
                        _generationState.value = GenerationState.Error("Please enter some study text or upload a PDF.")
                        return@launch
                    }
                    // Text-chunking safeguard: If input text > 20,000 characters, truncate it
                    val processedText = if (sourceText.length > 20000) {
                        sourceText.take(20000) + "\n\n(Note: Text was truncated to 20,000 characters to prevent API token limit errors.)"
                    } else {
                        sourceText
                    }
                    parts.add(Part(text = processedText))
                }

                _generationState.value = GenerationState.SendingToGemini

                val promptText = """
                    Analyze the provided material and generate:
                    1. A 'summary' array of exactly 3 bullet points summarizing the most critical concepts.
                    2. 5 to 10 high-quality flashcards (questions/terms on 'front', clear answers/explanations on 'back').
                    3. 5 to 10 interactive multiple-choice quiz questions. Each question must have 4 distinct options ('option1', 'option2', 'option3', 'option4'), a 'correctAnswer' (which must match one of the option fields EXACTLY), and a clear, helpful 'explanation'.
                    
                    You MUST respond with a single, strictly valid JSON object matching this schema exactly:
                    {
                      "title": "A concise, descriptive title for this study material",
                      "summary": [
                        "First high-impact key takeaway summary sentence",
                        "Second high-impact key takeaway summary sentence",
                        "Third high-impact key takeaway summary sentence"
                      ],
                      "flashcards": [
                        {
                          "front": "Front text",
                          "back": "Back text"
                        }
                      ],
                      "quiz": [
                        {
                          "question": "Question text",
                          "option1": "Choice A",
                          "option2": "Choice B",
                          "option3": "Choice C",
                          "option4": "Choice D",
                          "correctAnswer": "Choice A",
                          "explanation": "Why Choice A is correct"
                        }
                      ]
                    }
                    
                    Do not include any Markdown tags, HTML formatting, preambles, or markdown block quotes (such as ```json). Respond with only the raw JSON.
                """.trimIndent()

                parts.add(Part(text = promptText))

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = parts)),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.4f
                    )
                )

                // 60-second timeout wrapper to prevent hanging
                val response = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(60_000L) {
                        RetrofitClient.service.generateContent(
                            model = "gemini-3.5-flash",
                            apiKey = apiKey,
                            request = request
                        )
                    }
                }

                if (response == null) {
                    _generationState.value = GenerationState.Error("AI generation timed out (60s). Please try again with a simpler document or shorter text.")
                    return@launch
                }

                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText == null) {
                    _generationState.value = GenerationState.Error("Gemini returned an empty response. Try again.")
                    return@launch
                }

                // Sanitize response from potentially nested Markdown blocks
                val sanitizedJson = sanitizeJson(responseText)

                // Parse using Moshi adapter
                val studyResponse = try {
                    val adapter = RetrofitClient.moshi.adapter(GeminiStudyResponse::class.java)
                    withContext(Dispatchers.Default) {
                        adapter.fromJson(sanitizedJson)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (studyResponse == null) {
                    _generationState.value = GenerationState.Error("Failed to parse generated study set. Gemini response was malformed.")
                    return@launch
                }

                // Native Zod-equivalent Schema Validation
                var isValid = true
                var malformedReason = ""
                if (studyResponse.title.isBlank()) {
                    isValid = false
                    malformedReason = "Study title was missing."
                } else if (studyResponse.flashcards.isEmpty()) {
                    isValid = false
                    malformedReason = "No flashcards were generated."
                } else if (studyResponse.quiz.isEmpty()) {
                    isValid = false
                    malformedReason = "No quiz questions were generated."
                } else {
                    for (card in studyResponse.flashcards) {
                        if (card.front.isBlank() || card.back.isBlank()) {
                            isValid = false
                            malformedReason = "A generated flashcard had missing front or back text."
                            break
                        }
                    }
                    if (isValid) {
                        for (q in studyResponse.quiz) {
                            if (q.question.isBlank() || q.option1.isBlank() || q.option2.isBlank() || q.option3.isBlank() || q.option4.isBlank() || q.correctAnswer.isBlank()) {
                                isValid = false
                                malformedReason = "A generated quiz question had blank fields."
                                break
                            }
                            val options = listOf(q.option1, q.option2, q.option3, q.option4)
                            if (q.correctAnswer !in options) {
                                isValid = false
                                malformedReason = "A generated quiz question's correctAnswer did not match any of the four options exactly."
                                break
                            }
                        }
                    }
                }

                if (!isValid) {
                    _generationState.value = GenerationState.Error("Failed to generate study materials: $malformedReason Please try another topic or PDF.")
                    return@launch
                }

                // Save to Room DB
                val finalTitle = if (pdfUri != null && !pdfFileName.isNullOrEmpty()) {
                    pdfFileName.replace(".pdf", "", ignoreCase = true)
                } else {
                    studyResponse.title.ifEmpty { "Pasted Study Set" }
                }

                // Format the summary bullets nicely to store
                val summaryBulletsText = studyResponse.summary?.filter { it.isNotBlank() }?.joinToString("\n") 
                    ?: "• Key concept analysis of the material\n• Key terminology highlights\n• Comprehensive study summary"

                val generatedId = withContext(Dispatchers.IO) {
                    repository.saveGeneratedSet(
                        title = finalTitle,
                        sourceType = if (pdfUri != null) "pdf" else "text",
                        summaryPoints = summaryBulletsText,
                        flashcards = studyResponse.flashcards,
                        quizQuestions = studyResponse.quiz
                    )
                }

                _generationState.value = GenerationState.Success(generatedId)
            } catch (e: retrofit2.HttpException) {
                e.printStackTrace()
                val errorMsg = when (e.code()) {
                    400 -> "Invalid Gemini API Key or Token limit issue. Please check your document and credentials."
                    403 -> "API Key does not have permission to access the Gemini API. Please check your credentials."
                    429 -> "Gemini API rate limit exceeded. Please wait a minute before trying again."
                    else -> "Gemini API Error (HTTP ${e.code()}): ${e.message()}"
                }
                _generationState.value = GenerationState.Error(errorMsg)
            } catch (e: Exception) {
                e.printStackTrace()
                _generationState.value = GenerationState.Error("Error: ${e.localizedMessage ?: "Unknown parsing or network error"}")
            }
        }
    }

    private fun sanitizeJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        clean = clean.trim()
        
        // Ensure starting and ending bounds
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            clean = clean.substring(start, end + 1)
        }
        return clean
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            clearApiKey()
            _generationState.value = GenerationState.Idle
            navigateTo(Screen.Dashboard)
        }
    }

    suspend fun exportAllDataToJson(): String {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                val sets = db.studyDao().getAllStudySetsList()
                val exportList = mutableListOf<Map<String, Any>>()
                
                for (set in sets) {
                    val cards = db.studyDao().getFlashcardsForSetList(set.id)
                    val questions = db.studyDao().getQuizQuestionsForSetList(set.id)
                    
                    val setMap = mapOf(
                        "id" to set.id,
                        "title" to set.title,
                        "sourceType" to set.sourceType,
                        "createdAt" to set.createdAt,
                        "flashcards" to cards.map {
                            mapOf(
                                "id" to it.id,
                                "front" to it.front,
                                "back" to it.back,
                                "box" to it.box,
                                "nextReviewTime" to it.nextReviewTime,
                                "lastStudied" to it.lastStudied
                            )
                        },
                        "quiz" to questions.map {
                            mapOf(
                                "id" to it.id,
                                "question" to it.question,
                                "option1" to it.option1,
                                "option2" to it.option2,
                                "option3" to it.option3,
                                "option4" to it.option4,
                                "correctAnswer" to it.correctAnswer,
                                "explanation" to it.explanation,
                                "userSelection" to (it.userSelection ?: "")
                            )
                        }
                    )
                    exportList.add(setMap)
                }
                
                val adapter = RetrofitClient.moshi.adapter(Any::class.java)
                adapter.indent("  ").toJson(exportList)
            } catch (e: Exception) {
                e.printStackTrace()
                "{\"error\": \"Export failed: ${e.localizedMessage}\"}"
            }
        }
    }
}
