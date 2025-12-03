package com.example.askup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    private var role: String = "student"

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

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        LaunchedEffect(sessionId) {
            database.questionDao()
                .getQuestionsForSession(sessionId)
                .collect { list ->
                    questions = list
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
                        }
                    )
                }
            }
        ) {
            Scaffold (
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Lecturer view – $username") },
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
    }

    @Composable
    fun DrawerContent(
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit,
        onCloseDrawer: () -> Unit
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
                    onCheckedChange = { onToggleDarkMode() }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Current theme: ${if (isDarkMode) "Dark 🌙" else "Light ☀️"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
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
}