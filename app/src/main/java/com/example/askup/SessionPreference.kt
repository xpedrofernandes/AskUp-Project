package com.example.askup

import android.content.Context

/**
 * Stores and retrieves the current session information for each user.
 * Uses SharedPreferences so session details persist even if app is closed.
 */
object SessionPreference {

    // All stored values are kept under this SharedPreferences file name.
    private const val PREFS = "sessions"

    /**
     * Saves a session entry for a specific user:
     *  - session id: identifies which active session (lecture)
     *  - code: code student used to join (optional display / recovery)
     */
    fun saveSession(context: Context, userId: Int, sessionId: Int, code: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            // Key incorporates userId so multiple users can be stored on same device
            putInt("session_$userId", sessionId)
            putString("code_$userId", code)
            apply() // Apply asynchronously for better performance
        }
    }

    /**
     * Retrieves last saved session id for a given user.
     * Returns 0 if the student has not joined any session yet.
     */
    fun getSessionId(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt("session_$userId", 0)
    }

    /**
     * Retrieves the session code associated with the stored session.
     * Returns empty string as default.
     */
    fun getCode(context: Context, userId: Int): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("code_$userId", "") ?: ""
    }
}
