package com.example.askup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = intent.getStringExtra("username") ?: "Guest"

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome, $username",
                    style = MaterialTheme.typography.headlineMedium
                )

                Button(onClick = {
                    val intent = Intent(this@MainActivity, StudentActivity::class.java)
                    startActivity(intent)
                }) {
                    Text(text = "I'm a student")
                }

                Button(onClick = {
                    val intent = Intent(this@MainActivity, LecturerActivity::class.java)
                    startActivity(intent)
                }) {
                    Text(text = "I'm a lecturer")
                }
            }
        }
    }
}

