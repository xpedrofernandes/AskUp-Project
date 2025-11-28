package com.example.askup.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Track which users upvoted which questions
// Prevents users from upvoting the same question multiple times
@Entity(tableName = "user_upvotes")
data class UserUpvote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val questionId: Int
)