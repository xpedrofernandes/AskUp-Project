package com.example.askup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.Question
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LecturerActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private var sessionId: Int = 0
    private var userId: Int = 0
    private var username: String = "Lecturer"
    private var role: String = "student"   // 👈 default, will be overwritten by Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(applicationContext)

        sessionId = intent.getIntExtra("sessionId", 0)
        userId = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: "Lecturer"
        role = intent.getStringExtra("role") ?: "student"

        // 🚫 Hard guard: if this user is not a lecturer, close this screen
        if (role != "lecturer") {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LecturerScreen()
                }
            }
        }
    }

    @Composable
    fun LecturerScreen() {
        var questions by remember { mutableStateOf<List<Question>>(emptyList()) }

        var showAnswerDialog by remember { mutableStateOf(false) }
        var selectedQuestion by remember { mutableStateOf<Question?>(null) }
        var answerText by remember { mutableStateOf("") }

        // Observe questions for this session
        LaunchedEffect(sessionId) {
            database.questionDao()
                .getQuestionsForSession(sessionId)
                .collect { list ->
                    questions = list
                }
        }

        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Lecturer view – $username",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

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

                // Question text
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Meta row: upvotes + time
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

                // Status line: answered + pinned
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

                // Existing answer (if any)
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

                // Action buttons
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

    // -------- helper DB functions --------

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

    // time formatting helper (same style as StudentActivity)
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
