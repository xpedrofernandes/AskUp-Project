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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.User
import kotlinx.coroutines.launch

class RegisterActivity : ComponentActivity() {

    // Initialises a single instance of the Room database for this activity.
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // gets a handle to the shared database so we can insert new users.
        database = AppDatabase.getDatabase(applicationContext)

        // Sets up the Compose UI tree with theme support.
        setContent {
            // remembers the current theme preference across recompositions.
            var isDarkMode by remember { mutableStateOf(ThemePreference.isDarkMode(this)) }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // passes dark mode state and toggle into the registration screen.
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
        // Stores the raw text input from the username field.
        var username by remember { mutableStateOf("") }

        // holds the first password entry typed by the user.
        var password by remember { mutableStateOf("") }

        // keeps the repeated password to check if both match.
        var confirmPassword by remember { mutableStateOf("") }

        // default role is student; user can toggle to lecturer.
        var selectedRole by remember { mutableStateOf("student") }

        // collects any validation or registration error to display on screen.
        var errorMessage by remember { mutableStateOf("") }

        // controls the visibility of the instructions dialog from the drawer.
        var showInstructionsDialog by remember { mutableStateOf(false) }

        // Localised error strings for different validation scenarios.
        val errorUsernameRequired = stringResource(R.string.register_error_username_required)
        val errorPasswordRequired = stringResource(R.string.register_error_password_required)
        val errorPasswordMismatch = stringResource(R.string.register_error_password_mismatch)
        val errorPasswordShort = stringResource(R.string.register_error_password_short)

        // Drawer state manages open/close of the side navigation.
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Wraps the screen content with a modal navigation drawer.
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
                    // Configures the top app bar with back button and menu icon.
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.register_appbar_title)) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_content_description)
                                )
                            }
                        },
                        actions = {
                            // menu icon opens the drawer for accessibility and instructions.
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
                // Places registration fields in the centre of the screen.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // header text makes it clear this screen creates new users.
                    Text(
                        text = stringResource(R.string.register_header),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Username field used as unique identifier for login.
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.register_username_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )

                    // First password entry, hidden for privacy.
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.register_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )

                    // Second password entry used to confirm the first one.
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.register_confirm_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true
                    )

                    // Text label before the radio buttons to explain user roles.
                    Text(
                        text = stringResource(R.string.register_role_prompt),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Row of radio buttons to choose between student and lecturer role.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == "student",
                                onClick = { selectedRole = "student" }
                            )
                            Text(stringResource(R.string.register_role_student))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == "lecturer",
                                onClick = { selectedRole = "lecturer" }
                            )
                            Text(stringResource(R.string.register_role_lecturer))
                        }
                    }

                    // Shows validation feedback if something is wrong with the input.
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Main button validates input and, if correct, calls registerUser.
                    Button(
                        onClick = {
                            errorMessage = when {
                                username.isBlank() -> errorUsernameRequired
                                password.isBlank() -> errorPasswordRequired
                                password != confirmPassword -> errorPasswordMismatch
                                password.length < 4 -> errorPasswordShort
                                else -> {
                                    // input is valid here, so we proceed with Room insert.
                                    registerUser(username, password, selectedRole)
                                    ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(stringResource(R.string.register_button))
                    }

                    // Allows the user to cancel registration and go back to login.
                    TextButton(
                        onClick = { finish() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.register_login_prompt))
                    }
                }
            }
        }

        // instructions dialog provides a short explanation on how to use this screen.
        if (showInstructionsDialog) {
            AlertDialog(
                onDismissRequest = { showInstructionsDialog = false },
                title = { Text(stringResource(R.string.instructions_title)) },
                text = {
                    Text(stringResource(R.string.register_instructions))
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
        // Drawer content exposes settings and help before user is registered.
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

            // simple layout row for dark mode toggle option.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.dark_mode_label))
                    Text(
                        text = if (isDarkMode)
                            stringResource(R.string.dark_mode_enabled)
                        else
                            stringResource(R.string.dark_mode_disabled),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // switch that calls the shared toggle handler.
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // text feedback shows which mode is currently active.
            Text(
                text = stringResource(
                    if (isDarkMode) R.string.current_theme_dark else R.string.current_theme_light
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // clickable text opens the registration instructions dialog.
            Text(
                text = stringResource(R.string.instructions_title),
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
        // Uses lifecycleScope so database work runs off the main thread.
        lifecycleScope.launch {
            try {
                // checks if the username is already stored in the local Room table.
                if (database.userDao().usernameExists(username)) {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            getString(R.string.register_username_taken),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // builds a new user entity with the chosen role.
                val newUser = User(username = username, password = password, role = role)

                // inserts the new user into the database using the DAO.
                database.userDao().insertUser(newUser)

                // notifies user and returns to login screen on success.
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.register_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                // generic error message in case something unexpected happens.
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.register_error_generic, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
