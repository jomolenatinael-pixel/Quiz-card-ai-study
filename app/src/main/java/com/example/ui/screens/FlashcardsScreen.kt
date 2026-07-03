package com.example.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.FlashcardEntity
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    viewModel: StudyViewModel,
    onBackClick: () -> Unit
) {
    val flashcards by viewModel.currentFlashcards.collectAsState()
    val activeSet by viewModel.activeStudySet.collectAsState()

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    // TTS Setup
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        ttsInstance.language = Locale.getDefault()
        tts = ttsInstance

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        activeSet?.title ?: "Study Cards",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (flashcards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            if (isCompleted) {
                FlashcardSummaryView(
                    flashcards = flashcards,
                    onRestartStudy = {
                        currentIndex = 0
                        isFlipped = false
                        isCompleted = false
                    },
                    onTakeQuiz = {
                        activeSet?.let {
                            viewModel.navigateTo(Screen.Quiz(it.id))
                        }
                    },
                    onDone = onBackClick
                )
            } else {
                val card = flashcards.getOrNull(currentIndex) ?: return@Column

                // Progress Bar
                val progress = (currentIndex + 1).toFloat() / flashcards.size
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Card ${currentIndex + 1} of ${flashcards.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "SRS Box: ${card.box}/5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Flipping Card
                val rotationY by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "card_flip")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .graphicsLayer {
                            this.rotationY = rotationY
                            cameraDistance = 12 * density
                        }
                        .clickable { isFlipped = !isFlipped }
                        .testTag("study_flashcard_${card.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Card Text Content (Front vs Back based on rotation status)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (rotationY < 90f) {
                                // Front Text
                                Text(
                                    text = card.front,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.graphicsLayer {
                                        // prevent flipped front text
                                        this.rotationY = 0f
                                    }
                                )
                            } else {
                                // Back Text
                                Text(
                                    text = card.back,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.graphicsLayer {
                                        // rotate text 180 degrees back so it's upright
                                        this.rotationY = 180f
                                    }
                                )
                            }
                        }

                        // TTS Audio Play Button
                        IconButton(
                            onClick = {
                                if (isTtsReady) {
                                    val textToSpeak = if (!isFlipped) card.front else card.back
                                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .graphicsLayer {
                                    if (isFlipped) this.rotationY = 180f
                                }
                                .testTag("tts_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen Text",
                                tint = if (isFlipped) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Box Hint Indicator at bottom
                        Text(
                            text = if (!isFlipped) "TAP TO FLIP" else "TAP TO RETURN",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = (if (isFlipped) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.5f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .graphicsLayer {
                                    if (isFlipped) this.rotationY = 180f
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Leitner SRS Feedback Controls
                Text(
                    text = "Did you get this correct?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Incorrect Button
                    Button(
                        onClick = {
                            viewModel.updateFlashcardBox(card, correct = false)
                            advanceCard(
                                currentIndex = currentIndex,
                                size = flashcards.size,
                                onNext = { currentIndex = it },
                                onComplete = { isCompleted = true }
                            )
                            isFlipped = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("incorrect_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unknown", fontWeight = FontWeight.Bold)
                    }

                    // Correct Button
                    Button(
                        onClick = {
                            viewModel.updateFlashcardBox(card, correct = true)
                            advanceCard(
                                currentIndex = currentIndex,
                                size = flashcards.size,
                                onNext = { currentIndex = it },
                                onComplete = { isCompleted = true }
                            )
                            isFlipped = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("correct_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50) // Green indicator for success
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Known", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                PomodoroTimerWidget()
            }
        }
    }
}

private fun advanceCard(
    currentIndex: Int,
    size: Int,
    onNext: (Int) -> Unit,
    onComplete: () -> Unit
) {
    if (currentIndex < size - 1) {
        onNext(currentIndex + 1)
    } else {
        onComplete()
    }
}

@Composable
fun FlashcardSummaryView(
    flashcards: List<FlashcardEntity>,
    onRestartStudy: () -> Unit,
    onTakeQuiz: () -> Unit,
    onDone: () -> Unit
) {
    val boxCounts = remember(flashcards) {
        val counts = IntArray(6) { 0 }
        flashcards.forEach { card ->
            val b = card.box.coerceIn(1, 5)
            counts[b]++
        }
        counts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Session Complete",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Study Session Complete!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Fantastic work! The Leitner system updated your flashcard intervals based on your selections.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Leitner Box Status Board
        Text(
            "Leitner Box Distribution",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                for (box in 1..5) {
                    val count = boxCounts[box]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Box $box (Interval: ${getIntervalLabel(box)})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "$count cards",
                                color = if (count > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onRestartStudy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Study Again", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onTakeQuiz,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Take Set Quiz", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Dashboard", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun getIntervalLabel(box: Int): String {
    return when (box) {
        1 -> "30s"
        2 -> "2m"
        3 -> "10m"
        4 -> "1h"
        else -> "1d"
    }
}

@Composable
fun PomodoroTimerWidget() {
    var timerSeconds by remember { mutableIntStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isStudyMode by remember { mutableStateOf(true) } // true = Study (25m), false = Break (5m)
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning, isStudyMode) {
        if (isTimerRunning) {
            while (timerSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                timerSeconds--
            }
            isTimerRunning = false
            if (isStudyMode) {
                isStudyMode = false
                timerSeconds = 5 * 60
            } else {
                isStudyMode = true
                timerSeconds = 25 * 60
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pomodoro_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isStudyMode) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .testTag("pomodoro_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Timer Mode",
                        tint = if (isStudyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isStudyMode) "Pomodoro Study Focus (25m)" else "Rest Break Timer (5m)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (isExpanded) "▲ Hide" else "▼ Show",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val minutes = timerSeconds / 60
                    val seconds = timerSeconds % 60
                    val timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                    Text(
                        text = timeStr,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStudyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("pomodoro_timer_display")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isTimerRunning = false
                                isStudyMode = true
                                timerSeconds = 25 * 60
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStudyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isStudyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pomodoro_study_preset")
                        ) {
                            Text("Study (25m)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isTimerRunning = false
                                isStudyMode = false
                                timerSeconds = 5 * 60
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isStudyMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isStudyMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pomodoro_break_preset")
                        ) {
                            Text("Break (5m)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isTimerRunning = !isTimerRunning },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTimerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pomodoro_start_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTimerRunning) "Pause" else "Start Focus", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isTimerRunning = false
                                timerSeconds = if (isStudyMode) 25 * 60 else 5 * 60
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pomodoro_reset_button")
                        ) {
                            Text("Reset", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
