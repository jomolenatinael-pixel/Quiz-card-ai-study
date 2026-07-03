package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.QuizQuestionEntity
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: StudyViewModel,
    onBackClick: () -> Unit
) {
    val quizQuestions by viewModel.currentQuizQuestions.collectAsState()
    val activeSet by viewModel.activeStudySet.collectAsState()

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionText by remember { mutableStateOf<String?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    
    // Custom filter state
    var showOnlyMistakes by remember { mutableStateOf(false) }

    // Filter questions if "Review Mistakes" is active
    val filteredQuestions = remember(quizQuestions, showOnlyMistakes) {
        if (showOnlyMistakes) {
            quizQuestions.filter { q -> q.userSelection != q.correctAnswer }
        } else {
            quizQuestions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showOnlyMistakes) "Reviewing Mistakes" else (activeSet?.title ?: "Interactive Quiz"),
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
            if (quizQuestions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading interactive quiz...", fontWeight = FontWeight.Bold)
                }
                return@Scaffold
            }

            if (isCompleted || filteredQuestions.isEmpty()) {
                QuizResultView(
                    originalQuestions = quizQuestions,
                    onRetakeQuiz = {
                        viewModel.resetQuizAnswers(activeSet?.id ?: 0)
                        selectedOptionText = null
                        currentIndex = 0
                        showOnlyMistakes = false
                        isCompleted = false
                    },
                    onReviewMistakes = {
                        selectedOptionText = null
                        currentIndex = 0
                        showOnlyMistakes = true
                        isCompleted = false
                    },
                    onDone = onBackClick
                )
            } else {
                val question = filteredQuestions.getOrNull(currentIndex) ?: return@Column
                val isAnswered = question.userSelection != null

                // Progress Bar
                val progress = (currentIndex + 1).toFloat() / filteredQuestions.size
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${filteredQuestions.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

                // Question Statement Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Options List
                val options = listOf(question.option1, question.option2, question.option3, question.option4)
                options.forEach { optionText ->
                    QuizOptionRow(
                        optionText = optionText,
                        isSelected = selectedOptionText == optionText,
                        isAnswered = isAnswered,
                        isCorrectChoice = optionText == question.correctAnswer,
                        isUserSelected = optionText == question.userSelection,
                        onClick = {
                            if (!isAnswered) {
                                selectedOptionText = optionText
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Explanation Block (Animated Reveal)
                AnimatedVisibility(visible = isAnswered) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Explanation",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Actions
                if (!isAnswered) {
                    Button(
                        onClick = {
                            selectedOptionText?.let {
                                viewModel.selectQuizAnswer(question, it)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_answer_button"),
                        enabled = selectedOptionText != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit Answer", fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Button(
                        onClick = {
                            selectedOptionText = null
                            if (currentIndex < filteredQuestions.size - 1) {
                                currentIndex++
                            } else {
                                isCompleted = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("next_question_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        val label = if (currentIndex < filteredQuestions.size - 1) "Next Question" else "See Results"
                        Text(label, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuizOptionRow(
    optionText: String,
    isSelected: Boolean,
    isAnswered: Boolean,
    isCorrectChoice: Boolean,
    isUserSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isAnswered && isCorrectChoice -> Color(0xFFE8F5E9) // Success background
        isAnswered && isUserSelected && !isCorrectChoice -> Color(0xFFFFEBEE) // Error background
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isAnswered && isCorrectChoice -> Color(0xFF4CAF50)
        isAnswered && isUserSelected && !isCorrectChoice -> Color(0xFFE57373)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }

    val textColor = when {
        isAnswered && isCorrectChoice -> Color(0xFF2E7D32)
        isAnswered && isUserSelected && !isCorrectChoice -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isAnswered) { onClick() }
            .padding(16.dp)
            .testTag("quiz_option_$optionText"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Ring
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    1.5.dp,
                    if (isSelected || isUserSelected) borderColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    CircleShape
                )
                .background(
                    if (isSelected || (isAnswered && isUserSelected)) borderColor else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isAnswered && isCorrectChoice) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            } else if (isAnswered && isUserSelected && !isCorrectChoice) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = optionText,
            color = textColor,
            fontWeight = if (isSelected || (isAnswered && isCorrectChoice)) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuizResultView(
    originalQuestions: List<QuizQuestionEntity>,
    onRetakeQuiz: () -> Unit,
    onReviewMistakes: () -> Unit,
    onDone: () -> Unit
) {
    // Score Computations
    val total = originalQuestions.size
    val correctCount = originalQuestions.count { q -> q.userSelection == q.correctAnswer }
    val scorePercentage = if (total > 0) (correctCount * 100) / total else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Quiz Complete",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Quiz Finished!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        // High contrast score circle
        Card(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "$correctCount / $total",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$scorePercentage% Correct",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onRetakeQuiz,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake Quiz", fontWeight = FontWeight.Bold)
            }

            if (correctCount < total) {
                Button(
                    onClick = onReviewMistakes,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Review Mistakes Only", fontWeight = FontWeight.Bold)
                }
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
