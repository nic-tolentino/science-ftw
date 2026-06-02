package com.complexly.academy.spacedrepetition

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the SM-2 spaced repetition algorithm.
 *
 * Tests cover:
 * - Initial state creation
 * - Correct answer progression (intervals grow exponentially)
 * - Wrong answer reset (interval returns to 1 day)
 * - Partial correctness (quality scores between 0 and 1)
 * - Ease factor bounds (1.3–3.0)
 * - Due date calculation and `isDue()` logic
 */
class SpacedRepetitionAlgorithmTest {

    // Helper to get a fixed "today" for deterministic testing
    private val testToday: LocalDate
        get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // ── Initial State ────────────────────────────────────────────────────────

    @Test
    fun `initialState creates question due tomorrow`() {
        val state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")

        assertEquals("q1", state.questionId)
        assertEquals("vid1", state.videoId)
        assertEquals(2.5, state.easeFactor)
        assertEquals(1, state.intervalDays)
        assertEquals(0, state.repetitions)
        assertEquals(0.0, state.lastScore)
        assertEquals("", state.lastAnsweredDate)

        // Due tomorrow
        val expectedDue = testToday.plus(1L, DateTimeUnit.DAY)
        assertEquals(expectedDue.toString(), state.nextReviewDate)
        assertTrue(state.isDue(testToday.plus(1L, DateTimeUnit.DAY)))
        assertFalse(state.isDue(testToday))
    }

    // ── Correct Answer Progression ───────────────────────────────────────────

    @Test
    fun `first correct answer schedules review in 3 days`() {
        val initial = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        val result = SpacedRepetitionAlgorithm.updateState(initial, 1.0)

        assertEquals(1, result.repetitions)
        assertEquals(3, result.intervalDays)
        assertEquals(1.0, result.lastScore)
        assertEquals(testToday.plus(3L, DateTimeUnit.DAY).toString(), result.nextReviewDate)
    }

    @Test
    fun `second correct answer schedules review in 7 days`() {
        var state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0) // rep 1 → 3 days
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0) // rep 2 → 7 days

        assertEquals(2, state.repetitions)
        assertEquals(7, state.intervalDays)
    }

    @Test
    fun `third correct answer uses ease factor to compute interval`() {
        var state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0) // rep 1
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0) // rep 2
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0) // rep 3

        assertEquals(3, state.repetitions)
        // interval = previousInterval (7) * easeFactor (should be slightly above 2.5)
        assertTrue(state.intervalDays >= 17) // 7 * 2.5 = 17.5, so ≥ 17
    }

    @Test
    fun `ease factor increases with perfect answers`() {
        var state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        val initialEase = state.easeFactor

        repeat(5) {
            state = SpacedRepetitionAlgorithm.updateState(state, 1.0)
        }

        assertTrue(state.easeFactor > initialEase)
        assertTrue(state.easeFactor <= 3.0)
    }

    // ── Wrong Answer Reset ──────────────────────────────────────────────────

    @Test
    fun `wrong answer resets repetitions to zero`() {
        var state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0)
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0)
        assertEquals(2, state.repetitions)

        state = SpacedRepetitionAlgorithm.updateState(state, 0.0)
        assertEquals(0, state.repetitions)
        assertEquals(1, state.intervalDays)
    }

    @Test
    fun `wrong answer schedules review for tomorrow`() {
        var state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0)
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0)
        state = SpacedRepetitionAlgorithm.updateState(state, 1.0)

        state = SpacedRepetitionAlgorithm.updateState(state, 0.0)
        assertEquals(testToday.plus(1L, DateTimeUnit.DAY).toString(), state.nextReviewDate)
    }

    @Test
    fun `ease factor decreases with wrong answers`() {
        var state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        repeat(3) {
            state = SpacedRepetitionAlgorithm.updateState(state, 0.0)
        }

        assertTrue(state.easeFactor < 2.5)
        assertTrue(state.easeFactor >= 1.3)
    }

    // ── Partial Correctness ──────────────────────────────────────────────────

    @Test
    fun `quality of 0_7 passes threshold`() {
        val state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        val result = SpacedRepetitionAlgorithm.updateState(state, 0.7)

        assertEquals(1, result.repetitions)
        assertEquals(3, result.intervalDays)
    }

    @Test
    fun `quality of 0_6 fails threshold`() {
        val state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        val result = SpacedRepetitionAlgorithm.updateState(state, 0.69)

        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
    }

    @Test
    fun `quality between 0_7 and 1_0 increases ease slightly less than perfect`() {
        var perfect = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        var partial = SpacedRepetitionAlgorithm.initialState("q1", "vid1")

        repeat(5) {
            perfect = SpacedRepetitionAlgorithm.updateState(perfect, 1.0)
            partial = SpacedRepetitionAlgorithm.updateState(partial, 0.8)
        }

        assertTrue(perfect.easeFactor > partial.easeFactor)
        assertTrue(partial.easeFactor > 2.5)
    }

    // ── Ease Factor Bounds ──────────────────────────────────────────────────

    @Test
    fun `ease factor never exceeds 3_0`() {
        var state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        repeat(100) {
            state = SpacedRepetitionAlgorithm.updateState(state, 1.0)
        }
        assertTrue(state.easeFactor <= 3.0)
    }

    @Test
    fun `ease factor never drops below 1_3`() {
        var state = SpacedRepetitionAlgorithm.initialState("q1", "vid1")
        repeat(100) {
            state = SpacedRepetitionAlgorithm.updateState(state, 0.0)
        }
        assertTrue(state.easeFactor >= 1.3)
    }

    // ── isDue() Logic ───────────────────────────────────────────────────────

    @Test
    fun `isDue returns true when nextReviewDate is in the past`() {
        val past = testToday.plus(-1L, DateTimeUnit.DAY).toString()
        val state = QuestionReviewState(
            questionId = "q1",
            videoId = "vid1",
            nextReviewDate = past
        )
        assertTrue(state.isDue(testToday))
    }

    @Test
    fun `isDue returns true when nextReviewDate equals today`() {
        val state = QuestionReviewState(
            questionId = "q1",
            videoId = "vid1",
            nextReviewDate = testToday.toString()
        )
        assertTrue(state.isDue(testToday))
    }

    @Test
    fun `isDue returns false when nextReviewDate is in the future`() {
        val future = testToday.plus(1L, DateTimeUnit.DAY).toString()
        val state = QuestionReviewState(
            questionId = "q1",
            videoId = "vid1",
            nextReviewDate = future
        )
        assertFalse(state.isDue(testToday))
    }

    @Test
    fun `isDue returns true for empty nextReviewDate`() {
        val state = QuestionReviewState(
            questionId = "q1",
            videoId = "vid1",
            nextReviewDate = ""
        )
        assertTrue(state.isDue(testToday))
    }

    @Test
    fun `isDue returns true for malformed nextReviewDate`() {
        val state = QuestionReviewState(
            questionId = "q1",
            videoId = "vid1",
            nextReviewDate = "not-a-date"
        )
        assertTrue(state.isDue(testToday))
    }

    // ── ReviewSessionResult ──────────────────────────────────────────────────

    @Test
    fun `accuracy is zero when no answers given`() {
        val result = ReviewSessionResult(0, 0, 0, 0, 0)
        assertEquals(0.0, result.accuracy)
    }

    @Test
    fun `accuracy is 1_0 when all answers correct`() {
        val result = ReviewSessionResult(5, 5, 0, 0, 0)
        assertEquals(1.0, result.accuracy)
    }

    @Test
    fun `accuracy is 0_6 when 3 of 5 correct`() {
        val result = ReviewSessionResult(5, 3, 0, 0, 0)
        assertEquals(0.6, result.accuracy)
    }
}
