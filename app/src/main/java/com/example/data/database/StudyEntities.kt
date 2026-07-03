package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sets")
data class StudySetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceType: String, // "pdf" or "text"
    val createdAt: Long = System.currentTimeMillis(),
    val summaryPoints: String = "" // AI bullet-point summary
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setId: Long, // References StudySetEntity.id
    val front: String,
    val back: String,
    val box: Int = 1, // Leitner system box (1 to 5)
    val nextReviewTime: Long = System.currentTimeMillis(),
    val lastStudied: Long = 0
) {
    // Leitner system intervals (in milliseconds)
    // Box 1: 30 seconds (or immediate)
    // Box 2: 2 minutes
    // Box 3: 10 minutes
    // Box 4: 1 hour
    // Box 5: 1 day (or complete mastery)
    companion object {
        fun getNextReviewTime(box: Int): Long {
            val intervalMs = when (box) {
                1 -> 30_000L // 30 seconds
                2 -> 120_000L // 2 minutes
                3 -> 600_000L // 10 minutes
                4 -> 3600_000L // 1 hour
                else -> 86400_000L // 1 day
            }
            return System.currentTimeMillis() + intervalMs
        }
    }
}

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setId: Long, // References StudySetEntity.id
    val question: String,
    val option1: String,
    val option2: String,
    val option3: String,
    val option4: String,
    val correctAnswer: String,
    val explanation: String,
    val userSelection: String? = null // Store the last answered option (null means unanswered)
)
