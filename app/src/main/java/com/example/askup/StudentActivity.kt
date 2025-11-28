package com.example.askup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.askup.database.AppDatabase
import com.example.askup.database.Question
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StudentActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private var sessionId: Int = 0
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(applicationContext)

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

    @Composable
    fun StudentScreen(username: String) {
        var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
        var showDialog by remember { mutableStateOf(false) }

        LaunchedEffect(sessionId) {
            database.questionDao().getQuestionsForSession(sessionId).collect { questionList ->
                questions = questionList
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
                            QuestionCard(question)
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
                }
            )
        }
    }

    @Composable
    fun AskQuestionDialog(
        onDismiss: () -> Unit,
        onSubmit: (String) -> Unit
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
                    TextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = { Text("Type your question here...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
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
    fun QuestionCard(question: Question) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "👍 ${question.upvotes}",
                            style = MaterialTheme.typography.bodyMedium
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
                            text = "⭐ Highlighted",
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

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}