package com.example.data

import com.example.data.database.StudyDao
import com.example.data.database.StudySetEntity
import com.example.data.database.FlashcardEntity
import com.example.data.database.QuizQuestionEntity
import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyDao: StudyDao) {

    val allStudySets: Flow<List<StudySetEntity>> = studyDao.getAllStudySets()

    fun getFlashcards(setId: Long): Flow<List<FlashcardEntity>> {
        return studyDao.getFlashcardsForSet(setId)
    }

    suspend fun getFlashcardsList(setId: Long): List<FlashcardEntity> {
        return studyDao.getFlashcardsForSetList(setId)
    }

    fun getQuizQuestions(setId: Long): Flow<List<QuizQuestionEntity>> {
        return studyDao.getQuizQuestionsForSet(setId)
    }

    suspend fun getQuizQuestionsList(setId: Long): List<QuizQuestionEntity> {
        return studyDao.getQuizQuestionsForSetList(setId)
    }

    suspend fun saveGeneratedSet(
        title: String,
        sourceType: String,
        summaryPoints: String,
        flashcards: List<com.example.data.api.GeminiFlashcard>,
        quizQuestions: List<com.example.data.api.GeminiQuizQuestion>
    ): Long {
        val studySetId = studyDao.insertStudySet(
            StudySetEntity(
                title = title,
                sourceType = sourceType,
                summaryPoints = summaryPoints
            )
        )

        val flashcardEntities = flashcards.map { card ->
            FlashcardEntity(
                setId = studySetId,
                front = card.front,
                back = card.back
            )
        }
        studyDao.insertFlashcards(flashcardEntities)

        val quizEntities = quizQuestions.map { q ->
            QuizQuestionEntity(
                setId = studySetId,
                question = q.question,
                option1 = q.option1,
                option2 = q.option2,
                option3 = q.option3,
                option4 = q.option4,
                correctAnswer = q.correctAnswer,
                explanation = q.explanation
            )
        }
        studyDao.insertQuizQuestions(quizEntities)

        return studySetId
    }

    suspend fun updateFlashcard(flashcard: FlashcardEntity) {
        studyDao.updateFlashcard(flashcard)
    }

    suspend fun updateQuizQuestion(question: QuizQuestionEntity) {
        studyDao.updateQuizQuestion(question)
    }

    suspend fun resetQuizAnswers(setId: Long) {
        studyDao.resetQuizAnswers(setId)
    }

    suspend fun deleteStudySet(setId: Long) {
        studyDao.deleteFlashcardsForSet(setId)
        studyDao.deleteQuizQuestionsForSet(setId)
        studyDao.deleteStudySet(setId)
    }

    suspend fun clearAllData() {
        studyDao.deleteAllFlashcards()
        studyDao.deleteAllQuizQuestions()
        studyDao.deleteAllStudySets()
    }
}
