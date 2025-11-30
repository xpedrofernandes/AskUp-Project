package com.example.askup.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// DAO for user upvote operations
@Dao
interface UserUpvoteDao {

    // Check if a user has already upvoted a specific question
    @Query("SELECT EXISTS(SELECT 1 FROM user_upvotes WHERE userId = :userId AND questionId = :questionId)")
    suspend fun hasUserUpvoted(userId: Int, questionId: Int): Boolean

    // Record that a user upvoted a question
    @Insert
    suspend fun insertUpvote(upvote: UserUpvote)

    // Get all questions that a user has upvoted
    @Query("SELECT questionId FROM user_upvotes WHERE userId = :userId")
    fun getUpvotedQuestions(userId: Int): Flow<List<Int>>

    // Remove an upvote
    @Query("DELETE FROM user_upvotes WHERE userId = :userId AND questionId = :questionId")
    suspend fun removeUpvote(userId: Int, questionId: Int)

}