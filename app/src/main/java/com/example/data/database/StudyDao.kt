package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // --- Study Sets ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySet(studySet: StudySetEntity): Long

    @Query("SELECT * FROM study_sets ORDER BY createdAt DESC")
    fun getAllStudySets(): Flow<List<StudySetEntity>>

    @Query("SELECT * FROM study_sets ORDER BY createdAt DESC")
    suspend fun getAllStudySetsList(): List<StudySetEntity>

    @Query("SELECT * FROM study_sets WHERE id = :id")
    suspend fun getStudySetById(id: Long): StudySetEntity?

    @Query("DELETE FROM study_sets WHERE id = :setId")
    suspend fun deleteStudySet(setId: Long)

    // --- Flashcards ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Query("SELECT * FROM flashcards WHERE setId = :setId ORDER BY id ASC")
    fun getFlashcardsForSet(setId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE setId = :setId ORDER BY id ASC")
    suspend fun getFlashcardsForSetList(setId: Long): List<FlashcardEntity>

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE setId = :setId")
    suspend fun deleteFlashcardsForSet(setId: Long)

    // --- Quiz Questions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<QuizQuestionEntity>)

    @Query("SELECT * FROM quiz_questions WHERE setId = :setId ORDER BY id ASC")
    fun getQuizQuestionsForSet(setId: Long): Flow<List<QuizQuestionEntity>>

    @Query("SELECT * FROM quiz_questions WHERE setId = :setId ORDER BY id ASC")
    suspend fun getQuizQuestionsForSetList(setId: Long): List<QuizQuestionEntity>

    @Update
    suspend fun updateQuizQuestion(question: QuizQuestionEntity)

    @Query("DELETE FROM quiz_questions WHERE setId = :setId")
    suspend fun deleteQuizQuestionsForSet(setId: Long)
    
    // Reset quiz answers for a set
    @Query("UPDATE quiz_questions SET userSelection = NULL WHERE setId = :setId")
    suspend fun resetQuizAnswers(setId: Long)

    @Query("DELETE FROM study_sets")
    suspend fun deleteAllStudySets()

    @Query("DELETE FROM flashcards")
    suspend fun deleteAllFlashcards()

    @Query("DELETE FROM quiz_questions")
    suspend fun deleteAllQuizQuestions()
}
