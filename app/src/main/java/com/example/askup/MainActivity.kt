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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.Session
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private var sessionId: Int = 0
    private var sessionCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        database = AppDatabase.getDatabase(applicationContext)

        val username = intent.getStringExtra("username") ?: "Guest"
        val userId = intent.getIntExtra("userId", 0)
        val role = intent.getStringExtra("role") ?: "student"

        // Load saved session
        sessionId = SessionPreference.getSessionId(this, userId)
        sessionCode = SessionPreference.getCode(this, userId)

        NotificationHelper.createChannel(this)
        requestNotificationPermissionIfNeeded()

        setContent {
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
                        onLogout = { logoutAndReturnToLogin() }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(this, permission)

            if (granted != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    2001
                )
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
        var showFaqDialog by remember { mutableStateOf(false) }
        var showSessionDialog by remember { mutableStateOf(false) }
        var hasSession by remember { mutableStateOf(sessionId != 0) }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerContent(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onCloseDrawer = { scope.launch { drawerState.close() } },
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
                        title = { Text(stringResource(R.string.main_app_title)) },
                        navigationIcon = {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.main_welcome, username),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Text(
                        text = stringResource(
                            R.string.main_role_label,
                            role.replaceFirstChar { it.uppercase() }
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (role == "lecturer") {
                        // Lecturer view
                        if (hasSession) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Session Code:", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        sessionCode,
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(this@MainActivity, LecturerActivity::class.java)
                                    intent.putExtra("userId", userId)
                                    intent.putExtra("username", username)
                                    intent.putExtra("sessionId", sessionId)
                                    intent.putExtra("role", role)
                                    startActivity(intent)
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(text = stringResource(R.string.main_lecturer_button))
                            }

                            TextButton(onClick = { showSessionDialog = true }) {
                                Text("Create New Session")
                            }
                        } else {
                            Text("You need to create a session", modifier = Modifier.padding(bottom = 16.dp))
                            Button(
                                onClick = { showSessionDialog = true },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Create Session")
                            }
                        }

                    } else if (role == "student") {
                        // Student view
                        if (hasSession) {
                            Text(
                                "Joined Session: $sessionCode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Button(
                                onClick = {
                                    val intent = Intent(this@MainActivity, StudentActivity::class.java)
                                    intent.putExtra("userId", userId)
                                    intent.putExtra("username", username)
                                    intent.putExtra("sessionId", sessionId)
                                    intent.putExtra("role", role)
                                    startActivity(intent)
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(text = stringResource(R.string.main_student_button))
                            }

                            TextButton(onClick = { showSessionDialog = true }) {
                                Text("Join Different Session")
                            }
                        } else {
                            Text("Enter session code to join", modifier = Modifier.padding(bottom = 16.dp))
                            Button(
                                onClick = { showSessionDialog = true },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Join Session")
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.main_role_unknown),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (showFaqDialog) {
                    AlertDialog(
                        onDismissRequest = { showFaqDialog = false },
                        title = { Text(stringResource(R.string.drawer_faq)) },
                        text = { Text(text = stringResource(R.string.main_faq_text)) },
                        confirmButton = {
                            TextButton(onClick = { showFaqDialog = false }) {
                                Text(stringResource(R.string.close_button))
                            }
                        }
                    )
                }

                if (showSessionDialog) {
                    if (role == "lecturer") {
                        CreateSessionDialog(
                            userId = userId,
                            onDismiss = { showSessionDialog = false },
                            onCreated = { code ->
                                sessionCode = code
                                hasSession = true
                                showSessionDialog = false
                            }
                        )
                    } else {
                        JoinSessionDialog(
                            userId = userId,
                            onDismiss = { showSessionDialog = false },
                            onJoined = { code ->
                                sessionCode = code
                                hasSession = true
                                showSessionDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CreateSessionDialog(
        userId: Int,
        onDismiss: () -> Unit,
        onCreated: (String) -> Unit
    ) {
        var code by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Create Session") },
            text = {
                Column {
                    Text("Enter a 5-digit code for your session:", modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = code,
                        onValueChange = {
                            if (it.length <= 5 && it.all { char -> char.isDigit() }) {
                                code = it
                                error = ""
                            }
                        },
                        placeholder = { Text("12345") },
                        singleLine = true
                    )
                    if (error.isNotEmpty()) {
                        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (code.length != 5) {
                            error = "Code must be 5 digits"
                        } else {
                            createSession(userId, code) { success, message ->
                                if (success) {
                                    onCreated(code)
                                } else {
                                    error = message
                                }
                            }
                        }
                    },
                    enabled = code.length == 5
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    fun JoinSessionDialog(
        userId: Int,
        onDismiss: () -> Unit,
        onJoined: (String) -> Unit
    ) {
        var code by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Join Session") },
            text = {
                Column {
                    Text("Enter the 5-digit session code:", modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = code,
                        onValueChange = {
                            if (it.length <= 5 && it.all { char -> char.isDigit() }) {
                                code = it
                                error = ""
                            }
                        },
                        placeholder = { Text("12345") },
                        singleLine = true
                    )
                    if (error.isNotEmpty()) {
                        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (code.length != 5) {
                            error = "Code must be 5 digits"
                        } else {
                            joinSession(userId, code) { success, message ->
                                if (success) {
                                    onJoined(code)
                                } else {
                                    error = message
                                }
                            }
                        }
                    },
                    enabled = code.length == 5
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
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
                    onCheckedChange = { onToggleDarkMode() }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(
                    if (isDarkMode) R.string.current_theme_dark
                    else R.string.current_theme_light
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            Text(
                text = stringResource(R.string.drawer_faq),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        onFaqClicked()
                        onCloseDrawer()
                    }
            )

            Text(
                text = stringResource(R.string.drawer_logout),
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

    private fun createSession(userId: Int, code: String, callback: (Boolean, String) -> Unit) {
        lifecycleScope.launch {
            try {
                // Check if code already exists
                val existing = database.sessionDao().getSessionByCode(code)
                if (existing != null) {
                    runOnUiThread {
                        callback(false, "Code already in use")
                    }
                    return@launch
                }

                // Create new session
                val session = Session(
                    sessionCode = code,
                    lecturerId = userId,
                    sessionName = "Session $code",
                    isActive = true
                )
                val id = database.sessionDao().insertSession(session)

                // Save to preferences
                sessionId = id.toInt()
                SessionPreference.saveSession(this@MainActivity, userId, sessionId, code)

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Session created", Toast.LENGTH_SHORT).show()
                    callback(true, "Success")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    callback(false, "Error: ${e.message}")
                }
            }
        }
    }

    private fun joinSession(userId: Int, code: String, callback: (Boolean, String) -> Unit) {
        lifecycleScope.launch {
            try {
                // Find session with this code
                val session = database.sessionDao().getSessionByCode(code)

                if (session == null) {
                    runOnUiThread {
                        callback(false, "Invalid code")
                    }
                    return@launch
                }

                if (!session.isActive) {
                    runOnUiThread {
                        callback(false, "Session ended")
                    }
                    return@launch
                }

                // Save to preferences
                sessionId = session.sessionId
                SessionPreference.saveSession(this@MainActivity, userId, sessionId, code)

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Joined session", Toast.LENGTH_SHORT).show()
                    callback(true, "Success")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    callback(false, "Error: ${e.message}")
                }
            }
        }
    }

    private fun logoutAndReturnToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}