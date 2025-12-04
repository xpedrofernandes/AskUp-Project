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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    // Holds a reference to the Room database, shared across the activity.
    private lateinit var database: AppDatabase

    // Stores the current session id; used for filtering questions.
    private var sessionId: Int = 0

    // keeps track of which lecturer is viewing this screen.
    private var userId: Int = 0

    // simple copy of the lecturer name to show in the toolbar.
    private var username: String = "Lecturer"

    // role is passed in so the activity can guard against wrong access.
    private var role: String = "student"

    // Location client used to get the last known location for displaying city name.
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // gets the singleton Room database instance for queries and updates.
        database = AppDatabase.getDatabase(applicationContext)

        // Reads the session and user data that were sent from MainActivity.
        sessionId = intent.getIntExtra("sessionId", 0)
        userId = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: "Lecturer"
        role = intent.getStringExtra("role") ?: "student"

        // basic guard: if somehow a student opens this activity, just close it.
        if (role != "lecturer") {
            finish()
            return
        }

        // Initialise fused location client used for GPS / city name feature.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Sets up Compose UI with theme support and passes down dark mode state.
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
                            // when user toggles, flip the flag and persist it in preferences.
                            isDarkMode = !isDarkMode
                            ThemePreference.setDarkMode(this, isDarkMode)
                        }
                    )
                }
            }
        }
    }

    // Callbacks
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
            // Starts a fresh task so the user cannot navigate back into the session.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // Location helper used by both the initial load and the permission callback.
    // Fetches a friendly "City, Country" string and returns it via the onResult callback.
    private fun fetchCityName(onResult: (String) -> Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // if no location permission is granted, request it and fall back to "Unknown".
        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            onResult(getString(R.string.location_unknown))
            return
        }

        // Uses the last known location to avoid heavy GPS work.
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        // Geocoder translates latitude/longitude into a readable address.
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                        val address = addresses?.firstOrNull()
                        val city = address?.locality ?: address?.subAdminArea
                        val country = address?.countryCode

                        // Builds a label like "Dublin, IE" or falls back if data is missing.
                        val label = if (!city.isNullOrBlank() && !country.isNullOrBlank()) {
                            "$city, $country"
                        } else {
                            getString(R.string.location_unknown)
                        }
                        onResult(label)
                    } catch (e: Exception) {
                        // in case geocoder fails, keep the UI consistent with an unknown value.
                        onResult(getString(R.string.location_unknown))
                    }
                } else {
                    // no last location is available, so we just show the fallback.
                    onResult(getString(R.string.location_unknown))
                }
            }
            .addOnFailureListener {
                // if the location API fails, do not crash the UI – just show fallback.
                onResult(getString(R.string.location_unknown))
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
            // Once permission is granted, we can re-trigger the city lookup.
            fetchCityName { /* UI state is updated in the composable via callback. */ }
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
        // Holds the questions for this session, coming from the Room Flow.
        var questions by remember { mutableStateOf<List<Question>>(emptyList()) }

        // Controls whether the answer dialog is visible.
        var showAnswerDialog by remember { mutableStateOf(false) }

        // Keeps a reference to the question currently being edited.
        var selectedQuestion by remember { mutableStateOf<Question?>(null) }

        // stores the answer text being typed by the lecturer.
        var answerText by remember { mutableStateOf("") }

        // Instructions dialog state for the drawer "Instructions" item.
        var showInstructionsDialog by remember { mutableStateOf(false) }

        // City name shown under the title, populated from GPS.
        var cityName by remember { mutableStateOf("Locating...") }

        // Drawer state controls the hamburger menu sliding sheet.
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Collects all questions for the current session from the database.
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

        // Wraps the main UI in a navigation drawer for settings and logout.
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
                    // Top bar shows lecturer name and the city location under it.
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(
                                        id = R.string.lecturer_title_prefix,
                                        username
                                    )
                                )
                                Text(
                                    text = cityName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            // back arrow returns to main screen without logging out completely.
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            // menu button opens the drawer with dark mode and instructions.
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(
                                        id = R.string.menu_content_description
                                    )
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                // Main content column for lecturer tools.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    // short explanation of what the lecturer can do on this screen.
                    Text(
                        text = stringResource(id = R.string.lecturer_intro_text),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (questions.isEmpty()) {
                        // simple fallback if no one has asked anything yet.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.lecturer_no_questions),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        // List of all questions with pin/answer actions for each card.
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

        // Dialog used for adding or editing an answer to a selected question.
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
                title = { Text(stringResource(id = R.string.lecturer_instructions_title)) },
                text = {
                    Text(
                        text = stringResource(id = R.string.lecturer_instructions_text)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInstructionsDialog = false }) {
                        Text(stringResource(id = R.string.close_button))
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
        // Drawer for lecturer settings: dark mode, help, logout.
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Divider()

            Text(
                text = stringResource(id = R.string.accessibility_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Row grouping the dark mode label and the actual switch.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.dark_mode_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (isDarkMode)
                            stringResource(id = R.string.dark_mode_enabled)
                        else
                            stringResource(id = R.string.dark_mode_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Switch toggles theme and persists it via the callback.
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = {
                        onToggleDarkMode()
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Text label shows a quick summary of the current theme.
            Text(
                text = stringResource(
                    id = if (isDarkMode)
                        R.string.current_theme_dark
                    else
                        R.string.current_theme_light
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Instructions entry in the drawer.
            Text(
                text = stringResource(id = R.string.instructions_title),
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
                text = stringResource(id = R.string.drawer_logout),
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
        // Card summarises a single question with metadata and actions.
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // main question text as entered by the student.
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Shows current upvotes and posting time.
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

                // Displays status ("Answered" / "Pending") and pin state.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val statusText = if (question.isAnswered)
                        stringResource(id = R.string.student_status_answered)
                    else
                        stringResource(id = R.string.student_status_pending)

                    Text(
                        text = statusText,
                        color = if (question.isAnswered)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (question.isHighlighted) {
                        Text(
                            text = stringResource(id = R.string.student_card_pinned),
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // if the question already has an answer, show it under a label.
                if (question.answer != null) {
                    Text(
                        text = stringResource(id = R.string.lecturer_answer_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = question.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Action row: mark answered, pin/unpin, and add/edit answer.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // toggles the answered/pending flag in the database.
                    TextButton(onClick = onToggleAnswered) {
                        Text(
                            text = if (question.isAnswered)
                                stringResource(id = R.string.lecturer_mark_pending)
                            else
                                stringResource(id = R.string.lecturer_mark_answered)
                        )
                    }

                    // toggles the highlighted field so the question can be pinned.
                    TextButton(onClick = onTogglePinned) {
                        Text(
                            text = if (question.isHighlighted)
                                stringResource(id = R.string.lecturer_unpin)
                            else
                                stringResource(id = R.string.lecturer_pin)
                        )
                    }

                    // opens the dialog to enter or edit the text of an answer.
                    Button(onClick = onEditAnswer) {
                        Text(
                            text = if (question.answer == null)
                                stringResource(id = R.string.lecturer_add_answer)
                            else
                                stringResource(id = R.string.lecturer_edit_answer)

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
        // Dialog for typing the lecturer's answer with validation on empty text.
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(id = R.string.lecturer_answer_dialog_title)) },
            text = {
                Column {
                    // show the original question so the lecturer has context.
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
                    Text(stringResource(id = R.string.lecturer_answer_dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.lecturer_answer_dialog_cancel))
                }
            }
        )
    }

    private fun toggleAnswered(question: Question) {
        // uses coroutine scope attached to the lifecycle to update Room.
        lifecycleScope.launch {
            val updated = question.copy(isAnswered = !question.isAnswered)
            database.questionDao().updateQuestion(updated)
        }
    }

    private fun togglePin(question: Question) {
        // flips the highlighted flag in the database, used for pinning.
        lifecycleScope.launch {
            val newState = !question.isHighlighted
            database.questionDao().setHighlighted(question.questionId, newState)
        }
    }

    private fun setAnswer(questionId: Int, answer: String) {
        // saves or updates the answer text for a question in Room.
        lifecycleScope.launch {
            database.questionDao().answerQuestion(questionId, answer)
        }
    }

    private fun formatTime(timestamp: Long): String {
        // formats timestamps as HH:mm so they are easy to read during a lecture.
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    companion object {
        // Request code used for runtime location permission.
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
