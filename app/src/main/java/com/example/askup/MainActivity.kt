package com.example.askup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.Session
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private var testSessionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(applicationContext)

        // Get user data passed from LoginActivity via Intent
        val username = intent.getStringExtra("username") ?: "Guest"
        val userId = intent.getIntExtra("userId", 0)
        val role = intent.getStringExtra("role") ?: "student"

        // Create a test session for development purposes
        createTestSession(userId)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Welcome, $username!",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        Text(
                            text = "Role: ${role.capitalize()}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Button(
                            onClick = {
                                // Navigate to StudentActivity and pass data via Intent
                                val intent = Intent(this@MainActivity, StudentActivity::class.java)
                                intent.putExtra("userId", userId)  // Pass user ID
                                intent.putExtra("username", username)  // Pass username
                                intent.putExtra("sessionId", testSessionId)  // Pass session ID
                                startActivity(intent)
                            },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(text = "I'm a student")
                        }

                        Button(
                            onClick = {
                                // Navigate to LecturerActivity and pass data via Intent
                                val intent = Intent(this@MainActivity, LecturerActivity::class.java)
                                intent.putExtra("userId", userId)  // Pass user ID
                                intent.putExtra("username", username)  // Pass username
                                intent.putExtra("sessionId", testSessionId)  // Pass session ID
                                startActivity(intent)
                            },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(text = "I'm a lecturer")
                        }
                    }
                }
            }
        }
    }

    // Create a test session for development
    private fun createTestSession(userId: Int) {
        lifecycleScope.launch {
            // Check if test session already exists
            val existingSession = database.sessionDao().getSessionByCode("TEST123")

            if (existingSession == null) {
                // Create new test session
                val testSession = Session(
                    sessionCode = "TEST123",
                    lecturerId = userId,
                    sessionName = "CS301 - Software Development",
                    isActive = true
                )

                // Insert into database and get the session ID
                val sessionId = database.sessionDao().insertSession(testSession)
                testSessionId = sessionId.toInt()
            } else {
                // Use existing test session
                testSessionId = existingSession.sessionId
            }
        }
    }
}