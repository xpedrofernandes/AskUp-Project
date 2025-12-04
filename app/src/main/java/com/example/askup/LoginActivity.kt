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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    // Initialises the database so login checks can be made locally using Room.
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Loads the Room database instance for this Activity.
        database = AppDatabase.getDatabase(applicationContext)

        // Compose UI root setup with theme selection applied before screens render.
        setContent {
            var isDarkMode by remember { mutableStateOf(ThemePreference.isDarkMode(this)) }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Displays the composable responsible for the login UI.
                    LoginScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = {
                            // Saves preference so theme stays when app restarts.
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
        // Stores user input for the login attempt.
        val username = remember { mutableStateOf("") }
        val password = remember { mutableStateOf("") }

        // Holds a short error message for missing fields or failure to authenticate.
        val errorMessage = remember { mutableStateOf("") }

        val errorMissingFields = stringResource(R.string.error_enter_username_password)

        // controls the instructions pop-up.
        var showInstructionsDialog by remember { mutableStateOf(false) }

        // manages the drawer sliding animation and state.
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Wraps main screen with navigation drawer functionality.
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
                    // Provides the top bar with title and a menu icon to open the drawer.
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.login_title)) },
                        actions = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.menu_content_description)
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                // Main content of the login screen arranged vertically.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // friendly welcome text for first-time users.
                    Text(
                        text = stringResource(R.string.login_welcome),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    // Username field, used as primary identifier.
                    TextField(
                        value = username.value,
                        onValueChange = { username.value = it },
                        label = { Text(stringResource(R.string.username_label)) },
                        modifier = Modifier.padding(top = 16.dp),
                        singleLine = true
                    )

                    // Password field, secured visually.
                    TextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        label = { Text(stringResource(R.string.password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.padding(top = 8.dp),
                        singleLine = true
                    )

                    // Shows validation or authentication errors to guide the user.
                    if (errorMessage.value.isNotEmpty()) {
                        Text(
                            text = errorMessage.value,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Button attempts login only if required fields are filled.
                    Button(
                        onClick = {
                            if (username.value.isBlank() || password.value.isBlank()) {
                                // Keeps text assignment outside composable reads.
                                errorMessage.value = errorMissingFields
                            } else {
                                // Calls database authentication through repository functions.
                                loginUser(username.value, password.value)
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = stringResource(R.string.login_button))
                    }

                    // Navigation to registration page for new users.
                    TextButton(
                        onClick = {
                            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                            startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.login_register_prompt))
                    }
                }
            }
        }

        // Optional help dialog clarifying what to do on the screen.
        if (showInstructionsDialog) {
            AlertDialog(
                onDismissRequest = { showInstructionsDialog = false },
                title = { Text(stringResource(R.string.instructions_title)) },
                text = {
                    Text(stringResource(R.string.login_instructions_text))
                },
                confirmButton = {
                    TextButton(onClick = { showInstructionsDialog = false }) {
                        Text(stringResource(R.string.close_button))
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
        // Drawer gives accessibility options before login.
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Divider()

            Text(
                text = stringResource(R.string.accessibility_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Toggle for dark/light theme at login, saved immediately to preferences.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dark_mode_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (isDarkMode)
                            stringResource(R.string.dark_mode_enabled)
                        else
                            stringResource(R.string.dark_mode_disabled),
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

            // Quick theme feedback keeping the user aware visually.
            Text(
                text = stringResource(
                    if (isDarkMode) R.string.current_theme_dark else R.string.current_theme_light
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Opens instructional dialog.
            Text(
                text = stringResource(R.string.instructions_title),
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
        // Launches in coroutines to avoid blocking the UI while querying database.
        lifecycleScope.launch {
            try {
                // checks the local user table for matching credentials.
                val user = database.userDao().loginUser(username, password)

                if (user != null) {
                    // If login is valid, user object contains role and user ID.
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.login_welcome_back, user.username),
                            Toast.LENGTH_SHORT
                        ).show()

                        // Redirects to home screen with user context.
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("username", user.username)
                        intent.putExtra("userId", user.userId)
                        intent.putExtra("role", user.role)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // If credentials are invalid, gives user feedback.
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.login_invalid_credentials),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                // Shows a generic message if there is an unexpected failure.
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_error_generic, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
