package com.example.askup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
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

        val username = intent.getStringExtra("username") ?: "Guest"
        val userId = intent.getIntExtra("userId", 0)
        val role = intent.getStringExtra("role") ?: "student"

        // Creates or loads the shared test session for this module (navigation + DB usage).
        createTestSession(userId)

        setContent {
            // Persisted dark mode preference (state management + accessibility).
            var isDarkMode by remember { mutableStateOf(ThemePreference.isDarkMode(this)) }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        username = username,
                        userId = userId,
                        role = role,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = {
                            isDarkMode = !isDarkMode
                            ThemePreference.setDarkMode(this, isDarkMode)
                        },
                        // Logout sends the user back to LoginActivity and clears back stack.
                        onLogout = { logoutAndReturnToLogin() }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(
        username: String,
        userId: Int,
        role: String,
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit,
        onLogout: () -> Unit
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Simple state to control the FAQ dialog visibility (state management).
        var showFaqDialog by remember { mutableStateOf(false) }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerContent(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onCloseDrawer = {
                            scope.launch { drawerState.close() }
                        },
                        onLogout = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        },
                        onFaqClicked = {
                            scope.launch { drawerState.close() }
                            showFaqDialog = true
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("AskUp") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
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
                                intent.putExtra("role", role)
                                startActivity(intent)
                            },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(text = "Go to lecturer view")
                        }
                    } else {
                        Text(
                            text = "This role has no view configured.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Simple FAQ dialog (counts as extra UX/help feature).
                if (showFaqDialog) {
                    AlertDialog(
                        onDismissRequest = { showFaqDialog = false },
                        title = { Text("FAQ") },
                        text = {
                            Text(
                                "AskUp lets students submit and upvote questions in real time, " +
                                        "while lecturers can pin and answer the most important ones."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showFaqDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun DrawerContent(
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit,
        onCloseDrawer: () -> Unit,
        onLogout: () -> Unit,
        onFaqClicked: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Divider()

            Text(
                text = "Accessibility",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Dark mode toggle – accessibility + user preference.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dark Mode",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (isDarkMode) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Current theme: ${if (isDarkMode) "Dark 🌙" else "Light ☀️"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // FAQ entry – opens a simple help dialog.
            Text(
                text = "FAQ",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        onFaqClicked()
                        onCloseDrawer()
                    }
            )

            // Logout entry – clears back stack and returns to LoginActivity.
            Text(
                text = "Logout",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        onLogout()
                    }
            )
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

    private fun createTestSession(userId: Int) {
        lifecycleScope.launch {
            val existingSession = database.sessionDao().getSessionByCode("TEST123")

            if (existingSession == null) {
                val testSession = Session(
                    sessionCode = "TEST123",
                    lecturerId = userId,
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

    // Centralised logout: clears activity back stack and returns to LoginActivity.
    private fun logoutAndReturnToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
