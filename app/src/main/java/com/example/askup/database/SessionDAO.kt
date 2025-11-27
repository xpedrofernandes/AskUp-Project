package com.example.askup.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// DAO for session database operations
@Dao
interface SessionDao {

    // Create a new session (lecturer uses this)
    @Insert
    suspend fun insertSession(session: Session): Long

    // Get a specific session by its ID
    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: Int): Session?

    // Get a session by its unique code
    @Query("SELECT * FROM sessions WHERE sessionCode = :code")
    suspend fun getSessionByCode(code: String): Session?

    // Get all active sessions for a lecturer
    @Query("SELECT * FROM sessions WHERE lecturerId = :lecturerId AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveSessionsForLecturer(lecturerId: Int): Flow<List<Session>>

    // Get all sessions (both active and ended) for a lecturer
    @Query("SELECT * FROM sessions WHERE lecturerId = :lecturerId ORDER BY createdAt DESC")
    fun getAllSessionsForLecturer(lecturerId: Int): Flow<List<Session>>

    // End a session (mark as inactive)
    @Query("UPDATE sessions SET isActive = 0 WHERE sessionId = :sessionId")
    suspend fun endSession(sessionId: Int)

    // Update session details
    @Update
    suspend fun updateSession(session: Session)

    // Delete a session
    @Delete
    suspend fun deleteSession(session: Session)
}