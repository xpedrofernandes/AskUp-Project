package com.example.askup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.Question
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LecturerActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private var sessionId: Int = 0
    private var userId: Int = 0
    private var username: String = "Lecturer"
    private var role: String = "student"

    // Location client used to get the last known location for displaying city name.
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(applicationContext)

        sessionId = intent.getIntExtra("sessionId", 0)
        userId = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: "Lecturer"
        role = intent.getStringExtra("role") ?: "student"

        if (role != "lecturer") {
            finish()
            return
        }

        // Initialise fused location client used for GPS / city name feature.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            var isDarkMode by remember { mutableStateOf(ThemePreference.isDarkMode(this)) }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LecturerScreen(
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

    override fun onStart() {
        super.onStart()
        println("LecturerActivity: onStart - Activity is visible")
    }

    override fun onResume() {
        super.onResume()
        println("LecturerActivity: onResume - Activity is active")
    }

    override fun onPause() {
        super.onPause()
        println("LecturerActivity: onPause - Activity is partially visible")
    }

    override fun onStop() {
        super.onStop()
        println("LecturerActivity: onStop - Activity is no longer visible")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("LecturerActivity: onDestroy - Activity is being destroyed")
    }

    // Logs the lecturer out by clearing the back stack and sending them to LoginActivity.
    private fun logoutAndReturnToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // Location helper used by both the initial load and the permission callback.
    // Fetches a friendly "City, Country" string and returns it via the onResult callback.
    private fun fetchCityName(onResult: (String) -> Unit) {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            onResult("Unknown city")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                        val address = addresses?.firstOrNull()
                        val city = address?.locality ?: address?.subAdminArea
                        val country = address?.countryCode

                        val label = if (!city.isNullOrBlank() && !country.isNullOrBlank()) {
                            "$city, $country"
                        } else {
                            "Unknown city"
                        }
                        onResult(label)
                    } catch (e: Exception) {
                        onResult("Unknown city")
                    }
                } else {
                    onResult("Unknown city")
                }
            }
            .addOnFailureListener {
                onResult("Unknown city")
            }
    }

    // If the user grants permission after the system dialog, fetch the city name again.
    @Suppress("DEPRECATION") // Using legacy callback for runtime permissions handling.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // We cannot directly change Compose state here, so we just refresh the
            // last known location; the next time the Composable calls fetchCityName
            // it will resolve to the new permission state.
            fetchCityName { /* no-op here */ }
        } else {
            // Permission denied – optional: keep "Unknown city" label.
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LecturerScreen(
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit
    ) {
        var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
        var showAnswerDialog by remember { mutableStateOf(false) }
        var selectedQuestion by remember { mutableStateOf<Question?>(null) }
        var answerText by remember { mutableStateOf("") }

        // Instructions dialog state for the drawer "Instructions" item.
        var showInstructionsDialog by remember { mutableStateOf(false) }

        // City name shown under the title, populated from GPS.
        var cityName by remember { mutableStateOf("Locating...") }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        LaunchedEffect(sessionId) {
            database.questionDao()
                .getQuestionsForSession(sessionId)
                .collect { list ->
                    questions = list
                }
        }

        // Trigger location lookup once when the screen is first composed.
        LaunchedEffect(Unit) {
            fetchCityName { city ->
                cityName = city
            }
        }

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
                        onInstructionsClick = {
                            showInstructionsDialog = true
                        },
                        onLogoutClick = {
                            logoutAndReturnToLogin()
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Lecturer view – $username")
                                Text(
                                    text = cityName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Pin important questions and mark them as answered.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (questions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No questions yet.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(questions) { q ->
                                LecturerQuestionCard(
                                    question = q,
                                    onToggleAnswered = { toggleAnswered(q) },
                                    onTogglePinned = { togglePin(q) },
                                    onEditAnswer = {
                                        selectedQuestion = q
                                        answerText = q.answer ?: ""
                                        showAnswerDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAnswerDialog && selectedQuestion != null) {
            AnswerDialog(
                question = selectedQuestion!!,
                answerText = answerText,
                onTextChange = { answerText = it },
                onDismiss = { showAnswerDialog = false },
                onSave = {
                    setAnswer(selectedQuestion!!.questionId, answerText)
                    showAnswerDialog = false
                }
            )
        }

        // Simple Instructions dialog that explains how to use the lecturer view.
        if (showInstructionsDialog) {
            AlertDialog(
                onDismissRequest = { showInstructionsDialog = false },
                title = { Text("Instructions") },
                text = {
                    Text(
                        "Tap a question to add or edit an answer.\n\n" +
                                "Use the buttons on each card to mark questions as answered " +
                                "or pending, and to pin important questions.\n\n" +
                                "Use the Settings menu to switch between light and dark mode."
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
        onInstructionsClick: () -> Unit,
        onLogoutClick: () -> Unit
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

            // Instructions entry in the drawer.
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

            Spacer(modifier = Modifier.height(8.dp))

            // Logout entry in the drawer – clears the back stack and returns to LoginActivity.
            Text(
                text = "Logout",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        onCloseDrawer()
                        onLogoutClick()
                    }
            )
        }
    }

    @Composable
    fun LecturerQuestionCard(
        question: Question,
        onToggleAnswered: () -> Unit,
        onTogglePinned: () -> Unit,
        onEditAnswer: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "👍 ${question.upvotes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatTime(question.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (question.isAnswered) "Answered" else "Pending",
                        color = if (question.isAnswered)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (question.isHighlighted) {
                        Text(
                            text = "📌 Pinned",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (question.answer != null) {
                    Text(
                        text = "Answer:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = question.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onToggleAnswered) {
                        Text(
                            text = if (question.isAnswered) "Mark as pending"
                            else "Mark as answered"
                        )
                    }

                    TextButton(onClick = onTogglePinned) {
                        Text(
                            text = if (question.isHighlighted) "Unpin"
                            else "Pin"
                        )
                    }

                    Button(onClick = onEditAnswer) {
                        Text(
                            text = if (question.answer == null) "Add answer"
                            else "Edit answer"
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun AnswerDialog(
        question: Question,
        answerText: String,
        onTextChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onSave: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Answer question") },
            text = {
                Column {
                    Text(
                        text = question.questionText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = answerText,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = onSave, enabled = answerText.isNotBlank()) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun toggleAnswered(question: Question) {
        lifecycleScope.launch {
            val updated = question.copy(isAnswered = !question.isAnswered)
            database.questionDao().updateQuestion(updated)
        }
    }

    private fun togglePin(question: Question) {
        lifecycleScope.launch {
            val newState = !question.isHighlighted
            database.questionDao().setHighlighted(question.questionId, newState)
        }
    }

    private fun setAnswer(questionId: Int, answer: String) {
        lifecycleScope.launch {
            database.questionDao().answerQuestion(questionId, answer)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    companion object {
        // Request code used for runtime location permission.
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
