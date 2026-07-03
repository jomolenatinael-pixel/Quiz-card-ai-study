package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ApiKeyDialog
import com.example.ui.screens.CreateSetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: StudyViewModel = viewModel()
      val darkThemeEnabled by viewModel.darkThemeEnabled.collectAsState()
      
      ErrorBoundary {
        MyApplicationTheme(darkTheme = darkThemeEnabled) {
          val currentScreen by viewModel.currentScreen.collectAsState()
          val apiKey by viewModel.userApiKey.collectAsState()
          var showApiKeyDialog by remember { mutableStateOf(false) }

          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            
            when (val screen = currentScreen) {
              is Screen.Dashboard -> {
                DashboardScreen(
                  viewModel = viewModel,
                  onConfigureApiKey = { showApiKeyDialog = true },
                  onCreateSetClick = { viewModel.navigateTo(Screen.Create) }
                )
              }
              is Screen.Create -> {
                CreateSetScreen(
                  viewModel = viewModel,
                  onBackClick = { viewModel.navigateTo(Screen.Dashboard) }
                )
              }
              is Screen.Flashcards -> {
                FlashcardsScreen(
                  viewModel = viewModel,
                  onBackClick = { viewModel.navigateTo(Screen.Dashboard) }
                )
              }
              is Screen.Quiz -> {
                QuizScreen(
                  viewModel = viewModel,
                  onBackClick = { viewModel.navigateTo(Screen.Dashboard) }
                )
              }
              is Screen.PrivacyPolicy -> {
                PrivacyPolicyScreen(
                  onBackClick = { viewModel.navigateTo(Screen.Dashboard) }
                )
              }
              is Screen.AdminPortal -> {
                AdminPortalScreen(
                  viewModel = viewModel,
                  onBackClick = { viewModel.navigateTo(Screen.Dashboard) }
                )
              }
            }

            if (showApiKeyDialog) {
              ApiKeyDialog(
                currentKey = apiKey,
                onSave = { viewModel.saveApiKey(it) },
                onClear = { viewModel.clearApiKey() },
                onDismiss = { showApiKeyDialog = false },
                onPrivacyPolicyClick = { viewModel.navigateTo(Screen.PrivacyPolicy) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ErrorBoundary(content: @Composable () -> Unit) {
  var errorOccurred by remember { mutableStateOf(false) }
  var exception: Throwable? by remember { mutableStateOf(null) }

  if (errorOccurred) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Warning",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "A UI Error Occurred",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = exception?.localizedMessage ?: "An unexpected layout crash was intercepted by the ErrorBoundary.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = {
            errorOccurred = false
            exception = null
          }
        ) {
          Text("Reload App State")
        }
      }
    }
  } else {
    content()
  }
}

