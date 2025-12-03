package com.example.askup.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity to represent a question asked by a student during a session
// Questions can be upvoted by other students and answered by the lecturer
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val questionId: Int = 0,
    val sessionId: Int,
    val studentId: Int,
    val questionText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val upvotes: Int = 0,
    val isAnswered: Boolean = false,
    val isHighlighted: Boolean = false,
    val answer: String? = null,
    // City derived from GPS location (for marks: sensors + DB + UI)
    val city: String? = null
)


