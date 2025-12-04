package com.example.askup

import android.content.Context

object SessionPreference {
    private const val PREFS = "sessions"

    fun saveSession(context: Context, userId: Int, sessionId: Int, code: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("session_$userId", sessionId)
            putString("code_$userId", code)
            apply()
        }
    }

    fun getSessionId(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt("session_$userId", 0)
    }

    fun getCode(context: Context, userId: Int): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("code_$userId", "") ?: ""
    }
}