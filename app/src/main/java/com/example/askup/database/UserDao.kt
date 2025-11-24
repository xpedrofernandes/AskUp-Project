package com.example.askup.database

import androidx.room.*

// Basic DAO for user operations
@Dao
interface UserDao {
    
    // Register a new user
    @Insert
    suspend fun insertUser(user: User): Long
    
    // Login - get user by username and password
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun loginUser(username: String, password: String): User?
    
    // Check if username already exists
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    suspend fun usernameExists(username: String): Boolean
}
