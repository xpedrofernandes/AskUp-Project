package com.example.askup.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity to represent a question asked by a student during a session
// Questions can be upvoted by other students and answered by the lecturer
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true)
    val questionId: Int = 0,

    val sessionId: Int,  // Which session this question belongs to
    val studentId: Int,  // ID of the student who asked the question
    val questionText: String,
    val upvotes: Int = 0,
    val isAnswered: Boolean = false,  // Has the lecturer answered it?
    val isHighlighted: Boolean = false,  // Did lecturer mark it as important?
    val answer: String? = null,  // Lecturer's answer (null if not answered yet)
    val timestamp: Long = System.currentTimeMillis()  // When was the question asked
)