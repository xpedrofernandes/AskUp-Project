package com.example.askup.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity to represent a lecture session
// Each session is created by a lecturer and students can join it to ask questions
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Int = 0,

    val sessionCode: String,  // Unique code for students to join (will be shown in QR code)
    val lecturerId: Int,  // ID of who created this session
    val sessionName: String,  // Name like "CS301 - Lecture 5"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()  // session created on
)