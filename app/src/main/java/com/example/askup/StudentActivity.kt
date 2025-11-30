package com.example.askup

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.Question
import com.example.askup.database.UserUpvote
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var vibrator: Vibrator
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

        sessionId = intent.getIntExtra("sessionId", 0)
        userId = intent.getIntExtra("userId", 0)
        val username = intent.getStringExtra("username") ?: "Student"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudentScreen(username)
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

    @Composable
    fun StudentScreen(username: String) {
        var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
        var upvotedQuestions by remember { mutableStateOf<List<Int>>(emptyList()) }
        var showDialog by remember { mutableStateOf(false) }
        var showDetailsDialog by remember { mutableStateOf(false) }
        var selectedQuestion by remember { mutableStateOf<Question?>(null) }

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

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ask Question")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Questions",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (questions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No questions yet. Be the first to ask!",
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
            title = { Text("Ask a Question") },
            text = {
                Column {
                    Text(
                        text = "What would you like to ask?",
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
                            placeholder = { Text("Type or speak your question...") },
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
                    Text("Post")
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
    fun QuestionDetailsDialog(
        question: Question,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Question Details") },
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
                            text = "Upvotes:",
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
                            text = "Posted at:",
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
                            text = "Status:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = if (question.isAnswered) "Answered" else "Pending",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (question.isAnswered) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    if (question.isHighlighted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pinned:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "📌 Yes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (question.answer != null) {
                        Divider()
                        Text(
                            text = "Answer:",
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
                    Text("Close")
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
                            tint = if (hasUpvoted) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Text(
                            text = "${question.upvotes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasUpvoted) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    if (question.isAnswered) {
                        Text(
                            text = "✓ Answered",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (question.isHighlighted) {
                        Text(
                            text = "📌 Pinned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
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
                        text = "Answer:",
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question")
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
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
}