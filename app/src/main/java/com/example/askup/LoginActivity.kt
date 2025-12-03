package com.example.askup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(applicationContext)

        setContent {
            var isDarkMode by remember { mutableStateOf(ThemePreference.isDarkMode(this)) }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = {
                            isDarkMode = !isDarkMode
                            ThemePreference.setDarkMode(this, isDarkMode)
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginScreen(
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit
    ) {
        val username = remember { mutableStateOf("") }
        val password = remember { mutableStateOf("") }
        val errorMessage = remember { mutableStateOf("") }

        // Instructions dialog state for the drawer "Instructions" item.
        var showInstructionsDialog by remember { mutableStateOf(false) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerContent(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onCloseDrawer = { scope.launch { drawerState.close() } },
                        onInstructionsClick = { showInstructionsDialog = true }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("AskUp Login") },
                        actions = {
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
                        text = "Welcome to AskUp",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    TextField(
                        value = username.value,
                        onValueChange = { username.value = it },
                        label = { Text("Username") },
                        modifier = Modifier.padding(top = 16.dp),
                        singleLine = true
                    )

                    TextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.padding(top = 8.dp),
                        singleLine = true
                    )

                    if (errorMessage.value.isNotEmpty()) {
                        Text(
                            text = errorMessage.value,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (username.value.isBlank() || password.value.isBlank()) {
                                errorMessage.value = "Please enter username and password"
                            } else {
                                // Try to login with database
                                loginUser(username.value, password.value)
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = "Login")
                    }

                    TextButton(
                        onClick = {
                            val intent =
                                Intent(this@LoginActivity, RegisterActivity::class.java)
                            startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Don't have an account? Register")
                    }
                }
            }
        }

        // Simple instructions dialog for the login screen.
        if (showInstructionsDialog) {
            AlertDialog(
                onDismissRequest = { showInstructionsDialog = false },
                title = { Text("Instructions") },
                text = {
                    Text(
                        "Enter your AskUp username and password to sign in.\n\n" +
                                "Use the menu to switch between light and dark mode."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInstructionsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    @Composable
    fun DrawerContent(
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit,
        onCloseDrawer: () -> Unit,
        onInstructionsClick: () -> Unit
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
                    onCheckedChange = {
                        onToggleDarkMode()
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Current theme: ${if (isDarkMode) "Dark 🌙" else "Light ☀️"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            Text(
                text = "Instructions",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        onCloseDrawer()
                        onInstructionsClick()
                    }
            )
        }
    }

    private fun loginUser(username: String, password: String) {
        lifecycleScope.launch {
            try {
                val user = database.userDao().loginUser(username, password)

                if (user != null) {
                    // Login successful
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Welcome back, ${user.username}!",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("username", user.username)
                        intent.putExtra("userId", user.userId)
                        intent.putExtra("role", user.role)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // Login failed
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid username or password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
