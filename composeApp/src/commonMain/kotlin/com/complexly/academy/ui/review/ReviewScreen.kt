package com.complexly.academy.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.complexly.academy.model.Question
import com.complexly.academy.model.Video
import com.complexly.academy.spacedrepetition.ReviewSessionResult
import com.complexly.academy.ui.theme.*

/**
 * Banner shown on the Home screen when there are due reviews.
 */
@Composable
fun ReviewBanner(
    dueCount: Int,
    onStartReview: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CharcoalText
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dueCount review${if (dueCount != 1) "s" else ""} due",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reinforce what you've learned",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = onStartReview,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Review")
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

/**
 * Full-screen review session UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Start the review session when the screen is first shown
    LaunchedEffect(Unit) {
        viewModel.onSessionStarted()
    }

    // Show summary screen when session is complete
    val sessionResults = uiState.sessionResults
    if (!uiState.isSessionActive && sessionResults != null) {
        ReviewSummaryScreen(
            results = sessionResults,
            onContinue = { viewModel.onSessionComplete(); onNavigateBack() },
            modifier = modifier
        )
        return
    }

    // Handle "no reviews available" state
    if (!uiState.isSessionActive && uiState.dueReviews.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(CreamBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = CorrectGreen,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "All caught up!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CharcoalText
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No reviews are due right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediumGrayText
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNavigateBack) {
                Text("Go Back")
            }
        }
        return
    }

    // Loading state (only shown briefly while the first question loads)
    if (uiState.currentQuestion == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${uiState.currentIndex + 1}/${uiState.dueReviews.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Video context
            uiState.currentVideo?.let { video ->
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(16.dp))
            }

            // Question
            Text(
                text = uiState.currentQuestion!!.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CharcoalText
            )

            Spacer(Modifier.height(24.dp))

            // Options
            uiState.currentQuestion!!.options.forEachIndexed { index, option ->
                OptionButton(
                    option = option,
                    index = index,
                    isSelected = uiState.selectedAnswerIndex == index,
                    isSubmitted = uiState.isSubmitted,
                    onClick = { viewModel.onSelectAnswer(index) }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Explanation (shown after submit)
            if (uiState.isSubmitted) {
                val selectedOption = uiState.currentQuestion!!.options.getOrNull(uiState.selectedAnswerIndex ?: -1)
                val explanation = selectedOption?.explanation?.takeIf { it.isNotEmpty() }
                    ?: "The correct answer is: ${uiState.currentQuestion!!.options.find { it.isCorrect }?.text}"

                val isCorrect = selectedOption?.isCorrect == true

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) CorrectGreen.copy(alpha = 0.1f) else Color(0xFFE57373).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isCorrect) "✓ Correct!" else "✗ Not quite",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) CorrectGreen else Color(0xFFE57373)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalText
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Next button
                Button(
                    onClick = { viewModel.onNextQuestion() },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(if (uiState.currentIndex + 1 >= uiState.dueReviews.size) "Finish" else "Next", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                // Submit button
                Button(
                    onClick = { viewModel.onSubmitAnswer() },
                    enabled = uiState.selectedAnswerIndex != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Submit", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OptionButton(
    option: com.complexly.academy.model.Option,
    index: Int,
    isSelected: Boolean,
    isSubmitted: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSubmitted && option.isCorrect -> CorrectGreen
        isSubmitted && isSelected && !option.isCorrect -> Color(0xFFE57373)
        isSelected -> SelectedNavIcon
        else -> CardBorder
    }

    val backgroundColor = when {
        isSubmitted && option.isCorrect -> CorrectGreen.copy(alpha = 0.08f)
        isSubmitted && isSelected && !option.isCorrect -> Color(0xFFE57373).copy(alpha = 0.08f)
        isSelected -> SelectedNavIcon.copy(alpha = 0.05f)
        else -> Color.White
    }

    OutlinedButton(
        onClick = { if (!isSubmitted) onClick() },
        enabled = !isSubmitted,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = CharcoalText
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (isSubmitted) {
                Icon(
                    imageVector = if (option.isCorrect) Icons.Rounded.CheckCircle else if (isSelected) Icons.Rounded.Cancel else Icons.Rounded.Circle,
                    contentDescription = null,
                    tint = if (option.isCorrect) CorrectGreen else if (isSelected) Color(0xFFE57373) else MediumGrayText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ReviewSummaryScreen(
    results: ReviewSessionResult,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = CorrectGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Review Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CharcoalText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${results.correctCount} of ${results.totalAnswered} correct (${(results.accuracy * 100).toInt()}%)",
            style = MaterialTheme.typography.titleLarge,
            color = MediumGrayText
        )
        Spacer(Modifier.height(32.dp))

        // Schedule breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Rescheduled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                SummaryRow("Tomorrow", results.rescheduledTomorrow)
                SummaryRow("This week", results.rescheduledThisWeek)
                SummaryRow("Later", results.rescheduledLater)
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SummaryRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MediumGrayText)
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CharcoalText)
    }
}
