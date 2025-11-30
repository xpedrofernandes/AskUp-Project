package com.example.askup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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

        // Create / load a test session (shared by this module)
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
                            text = "Role: ${role.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 🔒 Only show the correct button for this role
                        if (role == "student") {
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        this@MainActivity,
                                        StudentActivity::class.java
                                    )
                                    intent.putExtra("userId", userId)
                                    intent.putExtra("username", username)
                                    intent.putExtra("sessionId", testSessionId)
                                    intent.putExtra("role", role)
                                    startActivity(intent)
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(text = "Go to student view")
                            }
                        } else if (role == "lecturer") {
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        this@MainActivity,
                                        LecturerActivity::class.java
                                    )
                                    intent.putExtra("userId", userId)
                                    intent.putExtra("username", username)
                                    intent.putExtra("sessionId", testSessionId)
                                    intent.putExtra("role", role) // 👈 pass role so LecturerActivity can double-check
                                    startActivity(intent)
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(text = "Go to lecturer view")
                            }
                        } else {
                            // fallback: if some unknown role, show nothing or a message
                            Text(
                                text = "This role has no view configured.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        println("MainActivity: onStart - Activity is visible")
    }

    override fun onResume() {
        super.onResume()
        println("MainActivity: onResume - Activity is active")
    }

    override fun onPause() {
        super.onPause()
        println("MainActivity: onPause - Activity is partially visible")
    }

    override fun onStop() {
        super.onStop()
        println("MainActivity: onStop - Activity is no longer visible")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("MainActivity: onDestroy - Activity is being destroyed")
    }
    // Create a test session for development
    private fun createTestSession(userId: Int) {
        lifecycleScope.launch {
            val existingSession = database.sessionDao().getSessionByCode("TEST123")

            if (existingSession == null) {
                val testSession = Session(
                    sessionCode = "TEST123",
                    lecturerId = userId, // first user who creates it becomes the owner
                    sessionName = "CS301 - Software Development",
                    isActive = true
                )

                val sessionId = database.sessionDao().insertSession(testSession)
                testSessionId = sessionId.toInt()
            } else {
                testSessionId = existingSession.sessionId
            }
        }
    }
}