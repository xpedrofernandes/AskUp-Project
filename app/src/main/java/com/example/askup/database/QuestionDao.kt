package com.example.askup.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// DAO for question database operations
@Dao
interface QuestionDao {

    // Add a new question to the database
    @Insert
    suspend fun insertQuestion(question: Question): Long

    // Get all questions for a specific session, sorted by upvotes (most popular first)
    @Query("SELECT * FROM questions WHERE sessionId = :sessionId ORDER BY upvotes DESC, timestamp DESC")
    fun getQuestionsForSession(sessionId: Int): Flow<List<Question>>

    // Get only unanswered questions for a session
    @Query("SELECT * FROM questions WHERE sessionId = :sessionId AND isAnswered = 0 ORDER BY upvotes DESC")
    fun getUnansweredQuestions(sessionId: Int): Flow<List<Question>>

    // Get only highlighted questions
    @Query("SELECT * FROM questions WHERE sessionId = :sessionId AND isHighlighted = 1 ORDER BY upvotes DESC")
    fun getHighlightedQuestions(sessionId: Int): Flow<List<Question>>

    // Get a specific question by its ID
    @Query("SELECT * FROM questions WHERE questionId = :questionId")
    suspend fun getQuestionById(questionId: Int): Question?

    // Upvote a question
    @Query("UPDATE questions SET upvotes = upvotes + 1 WHERE questionId = :questionId")
    suspend fun upvoteQuestion(questionId: Int)

    // Mark a question as answered with the answer text
    @Query("UPDATE questions SET isAnswered = 1, answer = :answer WHERE questionId = :questionId")
    suspend fun answerQuestion(questionId: Int, answer: String)

    // Toggle whether a question is highlighted
    @Query("UPDATE questions SET isHighlighted = :isHighlighted WHERE questionId = :questionId")
    suspend fun setHighlighted(questionId: Int, isHighlighted: Boolean)

    // Update entire question
    @Update
    suspend fun updateQuestion(question: Question)

    // Delete a question
    @Delete
    suspend fun deleteQuestion(question: Question)
}