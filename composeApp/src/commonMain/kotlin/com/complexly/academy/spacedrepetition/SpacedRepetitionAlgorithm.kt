package com.complexly.academy.spacedrepetition

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.Serializable

import com.complexly.academy.util.TimeOverride

private fun today(): LocalDate {
    return TimeOverride.effectiveToday()
}

/**
 * Tracks the spaced repetition state for a single question.
 * Uses the SM-2 algorithm (simplified) to schedule reviews.
 */
@Serializable
data class QuestionReviewState(
    val questionId: String,
    val videoId: String,
    val easeFactor: Double = 2.5,     // Range: 1.3–3.0, starts at 2.5
    val intervalDays: Int = 1,        // Days until next review
    val repetitions: Int = 0,         // Consecutive correct answers
    val nextReviewDate: String = "",  // YYYY-MM-DD
    val lastScore: Double = 0.0,      // 0.0–1.0, score of last attempt
    val lastAnsweredDate: String = "" // YYYY-MM-DD
) {
    /** Whether this question is due for review on or before the given date. */
    fun isDue(onDate: LocalDate = today()): Boolean {
        if (nextReviewDate.isEmpty()) return true
        return try {
            LocalDate.parse(nextReviewDate) <= onDate
        } catch (e: Exception) {
            true // Malformed date = treat as due
        }
    }
}

/** Aggregate statistics about review performance. */
data class ReviewStats(
    val totalQuestions: Int,
    val reviewedQuestions: Int,
    val dueToday: Int,
    val dueThisWeek: Int,
    val overallAccuracy: Double,        // 0.0–1.0
    val currentStreak: Int,             // Consecutive days with reviews
    val longestStreak: Int
)

/** Summary of a completed review session. */
data class ReviewSessionResult(
    val totalAnswered: Int,
    val correctCount: Int,
    val rescheduledTomorrow: Int,
    val rescheduledThisWeek: Int,
    val rescheduledLater: Int
) {
    val accuracy: Double
        get() = if (totalAnswered > 0) correctCount.toDouble() / totalAnswered else 0.0
}

object SpacedRepetitionAlgorithm {

    private const val MIN_EASE = 1.3
    private const val MAX_EASE = 3.0
    private const val PASS_THRESHOLD = 0.7

    /**
     * Updates the review state after answering a question.
     *
     * @param current The current review state
     * @param quality Score from 0.0 (completely wrong) to 1.0 (completely correct)
     * @return Updated review state with new interval and next review date
     */
    fun updateState(current: QuestionReviewState, quality: Double): QuestionReviewState {
        val today = today()

        // Ease factor adjustment (SM-2 formula)
        val newEase = (current.easeFactor + (quality - 0.7) * 0.5)
            .coerceIn(MIN_EASE, MAX_EASE)

        // Repetitions: increment on good answer, reset on poor answer
        val newRepetitions = if (quality >= PASS_THRESHOLD) {
            current.repetitions + 1
        } else {
            0
        }

        // Interval calculation
        val newInterval = when {
            newRepetitions == 0 -> 1          // Review tomorrow
            newRepetitions == 1 -> 3          // Review in 3 days
            newRepetitions == 2 -> 7          // Review in 1 week
            else -> (current.intervalDays * newEase).toInt().coerceAtLeast(1)
        }

        // Next review date
        val nextDate = today.plus(newInterval.toLong(), DateTimeUnit.DAY)

        return current.copy(
            easeFactor = newEase,
            intervalDays = newInterval,
            repetitions = newRepetitions,
            nextReviewDate = nextDate.toString(),
            lastScore = quality,
            lastAnsweredDate = today.toString()
        )
    }

    /**
     * Creates an initial review state for a question that the user has just learned
     * (e.g., by completing a video's proof-of-knowledge quiz).
     */
    fun initialState(questionId: String, videoId: String): QuestionReviewState {
        val today = today()
        return QuestionReviewState(
            questionId = questionId,
            videoId = videoId,
            easeFactor = 2.5,
            intervalDays = 1,
            repetitions = 0,
            nextReviewDate = today.plus(1L, DateTimeUnit.DAY).toString(), // First review tomorrow
            lastScore = 0.0,
            lastAnsweredDate = ""
        )
    }
}

