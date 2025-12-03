package com.example.askup

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.User
import kotlinx.coroutines.launch

class RegisterActivity : ComponentActivity() {

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
                    RegisterScreen(
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
    fun RegisterScreen(
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit
    ) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("student") }
        var errorMessage by remember { mutableStateOf("") }

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
                        title = { Text("Create account") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
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
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )

                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true
                    )

                    Text(
                        text = "I am a:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == "student",
                                onClick = { selectedRole = "student" }
                            )
                            Text("Student")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == "lecturer",
                                onClick = { selectedRole = "lecturer" }
                            )
                            Text("Lecturer")
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                username.isBlank() -> errorMessage = "Please enter a username"
                                password.isBlank() -> errorMessage = "Please enter a password"
                                password != confirmPassword -> errorMessage =
                                    "Passwords don't match"
                                password.length < 4 -> errorMessage =
                                    "Password must be at least 4 characters"
                                else -> registerUser(username, password, selectedRole)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Register")
                    }

                    TextButton(
                        onClick = { finish() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Already have an account? Login")
                    }
                }
            }
        }

        if (showInstructionsDialog) {
            AlertDialog(
                onDismissRequest = { showInstructionsDialog = false },
                title = { Text("Instructions") },
                text = { Text("Fill in your details and choose a role to create your account.") },
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
                    Text("Dark Mode")
                    Text(
                        text = if (isDarkMode) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall
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

            Text(
                text = "Instructions",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCloseDrawer()
                        onInstructionsClick()
                    }
                    .padding(vertical = 8.dp)
            )
        }
    }

    private fun registerUser(username: String, password: String, role: String) {
        lifecycleScope.launch {
            try {
                if (database.userDao().usernameExists(username)) {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Username already taken",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val newUser = User(username = username, password = password, role = role)
                database.userDao().insertUser(newUser)

                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
