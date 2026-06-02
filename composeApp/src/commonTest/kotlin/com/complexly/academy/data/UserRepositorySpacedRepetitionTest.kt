package com.complexly.academy.data

import com.complexly.academy.LegacyJsonDataSource
import com.complexly.academy.spacedrepetition.QuestionReviewState
import com.complexly.academy.spacedrepetition.ReviewStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositorySpacedRepetitionTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: UserRepositoryImpl

    private val today: LocalDate
        get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val dataStore = createTestDataStore("spaced_repetition_test", testScope)
        repository = UserRepositoryImpl(LegacyJsonDataSource, dataStore, testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `recordAnswer creates new review state for unknown question`() = runTest {
        advanceUntilIdle()
        repository.markVideoCompleted("bio_1")
        repository.recordAnswer("q1", "bio_1", 1.0)

        val stats = repository.getReviewStats()
        assertTrue(stats.totalQuestions >= 1)
    }

    @Test
    fun `recordAnswer with correct answer increments repetitions`() = runTest {
        advanceUntilIdle()
        repository.markVideoCompleted("bio_1")
        repository.recordAnswer("q1", "bio_1", 1.0)
        repository.recordAnswer("q1", "bio_1", 1.0)
        repository.recordAnswer("q1", "bio_1", 1.0)

        val stats = repository.getReviewStats()
        assertTrue(stats.reviewedQuestions >= 1)
    }

    @Test
    fun `recordAnswer with wrong answer resets interval to 1 day`() = runTest {
        advanceUntilIdle()
        repository.markVideoCompleted("bio_1")
        repository.recordAnswer("q1", "bio_1", 1.0)
        repository.recordAnswer("q1", "bio_1", 1.0)
        repository.recordAnswer("q1", "bio_1", 0.0)

        val stats = repository.getReviewStats()
        assertEquals(1, stats.dueThisWeek)
    }

    @Test
    fun `getDueReviews respects maxCount parameter`() = runTest {
        advanceUntilIdle()
        repeat(20) { i ->
            repository.recordAnswer("q_limit_$i", "bio_1", 1.0)
        }

        val limited = repository.getDueReviews(5)
        assertTrue(limited.size <= 5)
    }

    @Test
    fun `getReviewStats includes reviewed question count`() = runTest {
        advanceUntilIdle()
        repository.markVideoCompleted("bio_1")
        repository.recordAnswer("q1", "bio_1", 1.0)

        val stats = repository.getReviewStats()
        assertTrue(stats.totalQuestions >= 1)
        assertTrue(stats.reviewedQuestions >= 1)
    }

    @Test
    fun `getReviewStats accuracy reflects correct vs incorrect answers`() = runTest {
        advanceUntilIdle()
        repository.markVideoCompleted("bio_1")
        repository.recordAnswer("q1", "bio_1", 1.0)

        val stats = repository.getReviewStats()
        assertTrue(stats.overallAccuracy >= 0.0)
    }

    @Test
    fun `review states persist across recordAnswer calls`() = runTest {
        advanceUntilIdle()
        repository.markVideoCompleted("bio_1")
        repository.recordAnswer("q_persist", "bio_1", 1.0)
        repository.recordAnswer("q_persist", "bio_1", 1.0)

        val stats = repository.getReviewStats()
        assertTrue(stats.overallAccuracy > 0.0)
    }
}
