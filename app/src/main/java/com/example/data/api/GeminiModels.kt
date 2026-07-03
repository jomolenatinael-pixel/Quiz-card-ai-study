package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiFlashcard(
    val front: String,
    val back: String
)

@JsonClass(generateAdapter = true)
data class GeminiQuizQuestion(
    val question: String,
    val option1: String,
    val option2: String,
    val option3: String,
    val option4: String,
    val correctAnswer: String,
    val explanation: String
)

@JsonClass(generateAdapter = true)
data class GeminiStudyResponse(
    val title: String,
    val summary: List<String>?, // 3-bullet document summary
    val flashcards: List<GeminiFlashcard>,
    val quiz: List<GeminiQuizQuestion>
)

// --- Gemini API Schema Datatypes ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 representation
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null, // "application/json"
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)
