package com.complexly.academy.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.complexly.academy.model.Option
import com.complexly.academy.model.Question
import com.complexly.academy.ui.home.getChannelColor
import com.complexly.academy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.video?.title ?: "Question of the Day",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Quiz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val questions = uiState.questions
            if (questions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No questions available for this lesson", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (uiState.isQuizFinished) {
                QuizFinishedLayout(
                    correctCount = uiState.correctAnswersCount,
                    totalCount = questions.size,
                    onFinish = onNavigateBack
                )
            } else {
                val currentQuestion = questions.getOrNull(uiState.currentQuestionIndex)
                if (currentQuestion != null) {
                    val channelId = uiState.channelId
                    val channelColor = getChannelColor(channelId)
                    
                    QuizContent(
                        question = currentQuestion,
                        currentIndex = uiState.currentQuestionIndex,
                        totalCount = questions.size,
                        selectedOption = uiState.selectedOption,
                        isSubmitted = uiState.isSubmitted,
                        channelColor = channelColor,
                        onOptionSelected = { viewModel.selectOption(it) },
                        onSubmit = { viewModel.submitAnswer() },
                        onNext = { viewModel.nextQuestion() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun QuizContent(
    question: Question,
    currentIndex: Int,
    totalCount: Int,
    selectedOption: Option?,
    isSubmitted: Boolean,
    channelColor: Color,
    onOptionSelected: (Option) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUESTION ${currentIndex + 1} OF $totalCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MediumGrayText
            )
            Text(
                text = "${((currentIndex).toFloat() / totalCount * 100).toInt()}% Done",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CharcoalText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Linear Progress bar
        LinearProgressIndicator(
            progress = { (currentIndex).toFloat() / totalCount },
            color = channelColor,
            trackColor = CardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Question card
        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CharcoalText
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Multiple choice options
        question.options.forEach { option ->
            val isSelected = selectedOption == option
            val borderStroke = when {
                isSubmitted && option.isCorrect -> BorderStroke(2.dp, CorrectGreen)
                isSubmitted && isSelected && !option.isCorrect -> BorderStroke(2.dp, IncorrectRed)
                isSelected -> BorderStroke(2.dp, SelectedNavIcon)
                else -> BorderStroke(1.dp, OptionBorder)
            }
            val cardBg = when {
                isSubmitted && option.isCorrect -> CorrectGreenBg
                isSubmitted && isSelected && !option.isCorrect -> IncorrectRedBg
                isSelected -> SelectedNavIcon.copy(alpha = 0.05f)
                else -> Color.White
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = borderStroke,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !isSubmitted) { onOptionSelected(option) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option checkbox indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSubmitted && option.isCorrect -> CorrectGreen
                                    isSubmitted && isSelected && !option.isCorrect -> IncorrectRed
                                    isSelected -> SelectedNavIcon
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (isSelected || (isSubmitted && option.isCorrect)) 0.dp else 1.dp,
                                color = OptionBorder,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitted && option.isCorrect) {
                            Icon(Icons.Rounded.Check, contentDescription = "Correct", tint = Color.White, modifier = Modifier.size(16.dp))
                        } else if (isSubmitted && isSelected && !option.isCorrect) {
                            Icon(Icons.Rounded.Close, contentDescription = "Incorrect", tint = Color.White, modifier = Modifier.size(16.dp))
                        } else if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = option.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = CharcoalText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Explanation text after submitting
        if (isSubmitted && selectedOption != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ExplanationBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (selectedOption.isCorrect) "Correct!" else "Incorrect",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedOption.isCorrect) CorrectGreen else IncorrectRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedOption.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Bottom CTA Button
        val buttonText = when {
            !isSubmitted -> "Submit Answer"
            currentIndex == totalCount - 1 -> "Complete Quiz"
            else -> "Next Question"
        }
        val isButtonEnabled = selectedOption != null

        Button(
            onClick = {
                if (!isSubmitted) {
                    onSubmit()
                } else {
                    onNext()
                }
            },
            enabled = isButtonEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = SelectedNavIcon,
                disabledContainerColor = CardBorder
            ),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isButtonEnabled) Color.White else MediumGrayText
            )
        }
    }
}

@Composable
fun QuizFinishedLayout(
    correctCount: Int,
    totalCount: Int,
    onFinish: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(CorrectGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "Trophy Icon",
                    tint = CorrectGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quiz Completed!",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You got $correctCount out of $totalCount questions correct.",
                style = MaterialTheme.typography.bodyLarge,
                color = MediumGrayText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = SelectedNavIcon),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Return to Course",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
