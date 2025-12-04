package com.example.askup

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.Question
import com.example.askup.database.UserUpvote
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var vibrator: Vibrator
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var sessionId: Int = 0
    private var userId: Int = 0
    private var speechResultCallback: ((String) -> Unit)? = null

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            speechResultCallback?.invoke(spokenText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(applicationContext)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Location client for GPS / network location.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        sessionId = intent.getIntExtra("sessionId", 0)
        userId = intent.getIntExtra("userId", 0)
        val username = intent.getStringExtra("username") ?: "Student"

        // Initial text for location label
        val initialCityText = getString(R.string.location_locating)

        setContent {
            var isDarkMode by remember { mutableStateOf(ThemePreference.isDarkMode(this)) }
            var cityName by remember { mutableStateOf(initialCityText) }

            // When the screen is first composed, try to retrieve the city name.
            LaunchedEffect(Unit) {
                this@StudentActivity.fetchCityName { city ->
                    cityName = city
                }
            }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudentScreen(
                        username = username,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = {
                            isDarkMode = !isDarkMode
                            ThemePreference.setDarkMode(this, isDarkMode)
                        },
                        cityName = cityName
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        println("StudentActivity: onStart - Activity is visible")
    }

    override fun onResume() {
        super.onResume()
        println("StudentActivity: onResume - Activity is active and can receive input")
    }

    override fun onPause() {
        super.onPause()
        println("StudentActivity: onPause - Activity is partially visible")
    }

    override fun onStop() {
        super.onStop()
        println("StudentActivity: onStop - Activity is no longer visible")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("StudentActivity: onDestroy - Activity is being destroyed")
    }

    // Logs the user out by clearing the back stack and sending them to LoginActivity.
    private fun logoutAndReturnToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // Requests fine location permission if needed and retrieves the current city name.
    private fun fetchCityName(onResult: (String) -> Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            onResult(getString(R.string.location_unknown))
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
                        val city = addresses?.firstOrNull()?.locality
                        val country = addresses?.firstOrNull()?.countryCode
                        onResult(
                            when {
                                city != null && country != null -> "$city, $country"
                                city != null -> city
                                else -> getString(R.string.location_unknown)
                            }
                        )
                    } catch (e: Exception) {
                        onResult(getString(R.string.location_unknown))
                    }
                } else {
                    onResult(getString(R.string.location_unknown))
                }
            }
            .addOnFailureListener {
                onResult(getString(R.string.location_unknown))
            }
    }

    // If the user grants permission after the system dialog, fetch the city name again.
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
            fetchCityName { /* city is updated via LaunchedEffect next time */ }
        } else {
            // Permission denied – keep "Unknown city"
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StudentScreen(
        username: String,
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit,
        cityName: String
    ) {
        var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
        var upvotedQuestions by remember { mutableStateOf<List<Int>>(emptyList()) }
        var showDialog by remember { mutableStateOf(false) }
        var showDetailsDialog by remember { mutableStateOf(false) }
        var selectedQuestion by remember { mutableStateOf<Question?>(null) }

        // FAQ dialog state for the drawer "FAQ" item.
        var showFaqDialog by remember { mutableStateOf(false) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Load questions from database, ordered with pinned first, then upvotes
        LaunchedEffect(sessionId) {
            database.questionDao().getQuestionsForSession(sessionId).collect { questionList ->
                questions = questionList
            }
        }

        // Load which questions this user has upvoted
        LaunchedEffect(userId) {
            database.userUpvoteDao().getUpvotedQuestions(userId).collect { upvotedList ->
                upvotedQuestions = upvotedList
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
                        // Open FAQ dialog when FAQ is tapped in the drawer.
                        onFaqClick = {
                            showFaqDialog = true
                        },
                        // Logout from this activity when Logout is tapped in the drawer.
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
                                Text(stringResource(R.string.student_title))
                                Text(
                                    cityName,
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
                                    contentDescription = stringResource(
                                        R.string.menu_content_description
                                    )
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(
                                R.string.student_dialog_ask_title
                            )
                        )
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    if (questions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.student_no_questions),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(questions) { question ->
                                QuestionCard(
                                    question = question,
                                    hasUpvoted = upvotedQuestions.contains(question.questionId),
                                    onSwipeRight = { upvoteQuestion(question.questionId) },
                                    onLongPress = {
                                        selectedQuestion = question
                                        showDetailsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AskQuestionDialog(
                onDismiss = { showDialog = false },
                onSubmit = { questionText ->
                    postQuestion(questionText)
                    showDialog = false
                },
                onMicClick = { callback ->
                    startSpeechRecognition(callback)
                }
            )
        }

        if (showDetailsDialog && selectedQuestion != null) {
            QuestionDetailsDialog(
                question = selectedQuestion!!,
                onDismiss = { showDetailsDialog = false }
            )
        }

        // Simple FAQ dialog that explains how to use the student view.
        if (showFaqDialog) {
            AlertDialog(
                onDismissRequest = { showFaqDialog = false },
                title = { Text(stringResource(R.string.student_faq_title)) },
                text = {
                    Text(
                        stringResource(R.string.student_faq_text)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showFaqDialog = false }) {
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
        onFaqClick: () -> Unit,
        onLogoutClick: () -> Unit
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
                    onCheckedChange = {
                        onToggleDarkMode()
                    }
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

            // FAQ entry in the drawer – opens a help dialog for this activity.
            Text(
                text = stringResource(R.string.drawer_faq),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        onCloseDrawer()
                        onFaqClick()
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Logout entry in the drawer – clears the back stack and returns to LoginActivity.
            Text(
                text = stringResource(R.string.drawer_logout),
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
    fun AskQuestionDialog(
        onDismiss: () -> Unit,
        onSubmit: (String) -> Unit,
        onMicClick: ((String) -> Unit) -> Unit
    ) {
        var questionText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.student_dialog_ask_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.student_dialog_ask_prompt),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            placeholder = {
                                Text(stringResource(R.string.student_dialog_ask_placeholder))
                            },
                            modifier = Modifier.weight(1f),
                            minLines = 3
                        )

                        TextButton(
                            onClick = {
                                onMicClick { spokenText ->
                                    questionText = spokenText
                                }
                            }
                        ) {
                            Text(
                                text = "🎤",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (questionText.isNotBlank()) {
                            onSubmit(questionText)
                        }
                    },
                    enabled = questionText.isNotBlank()
                ) {
                    Text(stringResource(R.string.student_dialog_ask_post))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.student_dialog_ask_cancel))
                }
            }
        )
    }

    @Composable
    fun QuestionDetailsDialog(
        question: Question,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.student_details_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.student_details_upvotes),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${question.upvotes}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.student_details_posted_at),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = formatFullTime(question.timestamp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.student_details_status),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = if (question.isAnswered)
                                stringResource(R.string.student_status_answered)
                            else
                                stringResource(R.string.student_status_pending),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (question.isAnswered)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Gray
                        )
                    }

                    if (question.isHighlighted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.student_details_pinned),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "📌 " + stringResource(R.string.student_details_pinned_yes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (question.answer != null) {
                        Divider()
                        Text(
                            text = stringResource(R.string.student_details_answer_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = question.answer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close_button))
                }
            }
        )
    }

    @Composable
    fun QuestionCard(
        question: Question,
        hasUpvoted: Boolean,
        onSwipeRight: () -> Unit,
        onLongPress: () -> Unit
    ) {
        var offsetX by remember { mutableStateOf(0f) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > 200f) {
                                onSwipeRight()
                            }
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX += dragAmount
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Upvote icon and count – clickable row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onSwipeRight() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Upvotes",
                            modifier = Modifier.size(20.dp),
                            tint = if (hasUpvoted)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Gray
                        )
                        Text(
                            text = "${question.upvotes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasUpvoted)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Gray
                        )
                    }

                    if (question.isAnswered) {
                        Text(
                            text = stringResource(R.string.student_card_answered),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (question.isHighlighted) {
                        Text(
                            text = stringResource(R.string.student_card_pinned),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.typography.bodySmall.color
                        )
                    }

                    Text(
                        text = formatTime(question.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (question.answer != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(R.string.student_details_answer_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = question.answer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    private fun startSpeechRecognition(callback: (String) -> Unit) {
        speechResultCallback = callback

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.speech_not_available),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun postQuestion(questionText: String) {
        lifecycleScope.launch {
            val newQuestion = Question(
                sessionId = sessionId,
                studentId = userId,
                questionText = questionText
            )
            database.questionDao().insertQuestion(newQuestion)
        }
    }

    private fun upvoteQuestion(questionId: Int) {
        lifecycleScope.launch {
            val alreadyUpvoted = database.userUpvoteDao().hasUserUpvoted(userId, questionId)

            if (alreadyUpvoted) {
                // REMOVE upvote: delete row + decrement counter
                database.userUpvoteDao().removeUpvote(userId, questionId)
                database.questionDao().removeUpvote(questionId)

                // Optional: small vibration for feedback
                if (vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                30,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(30)
                    }
                }

            } else {
                // ADD upvote: insert row + increment counter
                val upvote = UserUpvote(
                    userId = userId,
                    questionId = questionId
                )
                database.userUpvoteDao().insertUpvote(upvote)
                database.questionDao().upvoteQuestion(questionId)

                if (vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                50,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                }
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatFullTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
