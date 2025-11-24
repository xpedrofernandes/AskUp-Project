package com.example.askup.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Simple user entity - we can add more fields later as needed
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val username: String,
    val password: String,
    val role: String  // "student" or "lecturer"
)
