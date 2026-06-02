package com.complexly.academy

import com.complexly.academy.data.UserRepositoryImpl
import com.complexly.academy.data.createTestDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var userRepository: UserRepositoryImpl

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val dataStore = createTestDataStore("user_repo_test", testScope)
        userRepository = UserRepositoryImpl(LegacyJsonDataSource, dataStore, testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── LearningProgress ──────────────────────────────────────────────────

    @Test
    fun `learningProgress starts with empty completedVideoIds`() = runTest {
        val progress = userRepository.learningProgress.first { it.streakCount == 6 }
        assertTrue(progress.completedVideoIds.isEmpty())
        assertTrue(progress.completedCourseIds.isEmpty())
    }

    @Test
    fun `markVideoCompleted adds to completedVideoIds`() = runTest {
        userRepository.markVideoCompleted("bio_1")
        val progress = userRepository.learningProgress.first { it.completedVideoIds.contains("bio_1") }
        assertTrue(progress.completedVideoIds.contains("bio_1"))
    }

    @Test
    fun `markVideoCompleted completing all playlist videos marks course complete`() = runTest {
        // LegacyData has playlist "biology" with videos bio_1 and bio_2
        // Both videos have playlistIds = ["biology"], so course completion should trigger
        userRepository.markVideoCompleted("bio_1")
        userRepository.learningProgress.first { it.completedVideoIds.contains("bio_1") }
        assertTrue(userRepository.learningProgress.value.completedCourseIds.isEmpty())

        userRepository.markVideoCompleted("bio_2")
        val progress = userRepository.learningProgress.first { it.completedCourseIds.contains("biology") }
        assertTrue(progress.completedCourseIds.contains("biology"))
    }

    @Test
    fun `markVideoCompleted is idempotent`() = runTest {
        userRepository.markVideoCompleted("bio_1")
        userRepository.markVideoCompleted("bio_1")
        val progress = userRepository.learningProgress.first { it.completedVideoIds.contains("bio_1") }
        assertEquals(1, progress.completedVideoIds.size)
    }

    @Test
    fun `updateStreak increments streak count`() = runTest {
        val initial = userRepository.learningProgress.first { it.streakCount == 6 }.streakCount
        userRepository.updateStreak(increment = true)
        val updated = userRepository.learningProgress.first { it.streakCount == 7 }
        assertEquals(initial + 1, updated.streakCount)
    }

    @Test
    fun `updateStreak with false does not change streak`() = runTest {
        userRepository.learningProgress.first { it.streakCount == 6 }
        userRepository.updateStreak(increment = false)
        // Give it a moment to process
        val progress = userRepository.learningProgress.value
        assertEquals(6, progress.streakCount)
    }

    // ── UserPreferences ───────────────────────────────────────────────────

    @Test
    fun `userPreferences starts with defaults`() = runTest {
        val prefs = userRepository.userPreferences.value
        assertFalse(prefs.darkModeEnabled)
        assertTrue(prefs.pushNotificationsEnabled)
        assertTrue(prefs.disabledChannelIds.isEmpty())
    }

    @Test
    fun `setDarkMode persists through DataStore`() = runTest {
        userRepository.setDarkMode(true)
        val prefs = userRepository.userPreferences.first { it.darkModeEnabled }
        assertTrue(prefs.darkModeEnabled)

        userRepository.setDarkMode(false)
        val updated = userRepository.userPreferences.first { !it.darkModeEnabled }
        assertFalse(updated.darkModeEnabled)
    }

    @Test
    fun `setPushNotifications persists through DataStore`() = runTest {
        userRepository.setPushNotifications(false)
        val prefs = userRepository.userPreferences.first { !it.pushNotificationsEnabled }
        assertFalse(prefs.pushNotificationsEnabled)
    }

    @Test
    fun `setDisabledChannels persists and replaces previous channels`() = runTest {
        val newChannels = setOf("crashcourse")
        userRepository.setDisabledChannels(newChannels)
        val prefs = userRepository.userPreferences.first { it.disabledChannelIds == newChannels }
        assertEquals(newChannels, prefs.disabledChannelIds)
        assertTrue(prefs.disabledChannelIds.contains("crashcourse"))
        assertFalse(prefs.disabledChannelIds.contains("scishow"))
    }

    @Test
    fun `learningProgress and userPreferences emit independently`() = runTest {
        // Modifying preferences should not affect learning progress
        val initialProgress = userRepository.learningProgress.value
        userRepository.setDarkMode(true)
        userRepository.userPreferences.first { it.darkModeEnabled }

        // Learning progress unchanged
        assertEquals(initialProgress.streakCount, userRepository.learningProgress.value.streakCount)
        assertEquals(initialProgress.completedVideoIds, userRepository.learningProgress.value.completedVideoIds)
    }

    @Test
    fun `enrollInCourse adds courseId to enrolledCourseIds`() = runTest {
        userRepository.enrollInCourse("course_abc")
        val progress = userRepository.learningProgress.first { it.enrolledCourseIds.contains("course_abc") }
        assertTrue(progress.enrolledCourseIds.contains("course_abc"))
    }

    @Test
    fun `markVideoWatched adds videoId to recentlyWatchedVideoIds and maintains order and cap`() = runTest {
        userRepository.markVideoWatched("vid_1")
        userRepository.markVideoWatched("vid_2")
        userRepository.markVideoWatched("vid_1") // moving vid_1 back to front

        val progress = userRepository.learningProgress.first { it.recentlyWatchedVideoIds.contains("vid_1") }
        assertEquals(listOf("vid_1", "vid_2"), progress.recentlyWatchedVideoIds)

        // Verify cap at 10 items
        for (i in 3..15) {
            userRepository.markVideoWatched("vid_$i")
        }
        val cappedProgress = userRepository.learningProgress.first { it.recentlyWatchedVideoIds.size == 10 }
        assertEquals(10, cappedProgress.recentlyWatchedVideoIds.size)
        assertEquals("vid_15", cappedProgress.recentlyWatchedVideoIds.first())
    }

    @Test
    fun `markVideoCompleted adds videoId to recentlyCompletedVideoIds`() = runTest {
        userRepository.markVideoCompleted("bio_1")
        val progress = userRepository.learningProgress.first { it.recentlyCompletedVideoIds.contains("bio_1") }
        assertTrue(progress.recentlyCompletedVideoIds.contains("bio_1"))
    }

    @Test
    fun `unenrollFromCourse removes courseId from enrolledCourseIds`() = runTest {
        userRepository.enrollInCourse("course_abc")
        val progressEnrolled = userRepository.learningProgress.first { it.enrolledCourseIds.contains("course_abc") }
        assertTrue(progressEnrolled.enrolledCourseIds.contains("course_abc"))

        userRepository.unenrollFromCourse("course_abc")
        val progressUnenrolled = userRepository.learningProgress.first { !it.enrolledCourseIds.contains("course_abc") }
        assertFalse(progressUnenrolled.enrolledCourseIds.contains("course_abc"))
    }

    @Test
    fun `incrementCorrectAnswers increments correct answers counter`() = runTest {
        val initialCount = userRepository.learningProgress.value.correctAnswersCount
        userRepository.incrementCorrectAnswers()
        val updatedProgress = userRepository.learningProgress.first { it.correctAnswersCount == initialCount + 1 }
        assertEquals(initialCount + 1, updatedProgress.correctAnswersCount)
    }

    @Test
    fun `markPlaylistViewed adds playlistId to recentlyViewedItemIds`() = runTest {
        userRepository.markPlaylistViewed("biology")
        val progress = userRepository.learningProgress.first { it.recentlyViewedItemIds.contains("playlist:biology") }
        assertTrue(progress.recentlyViewedItemIds.contains("playlist:biology"))
    }

    @Test
    fun `markVideoWatched also adds video to recentlyViewedItemIds`() = runTest {
        userRepository.markVideoWatched("bio_1")
        val progress = userRepository.learningProgress.first { it.recentlyViewedItemIds.contains("video:bio_1") }
        assertTrue(progress.recentlyViewedItemIds.contains("video:bio_1"))
    }

    @Test
    fun `recentlyViewedItemIds maintains cap at 20 and preserves order`() = runTest {
        // Add 25 items
        for (i in 1..25) {
            userRepository.markPlaylistViewed("playlist_$i")
        }
        val progress = userRepository.learningProgress.first { it.recentlyViewedItemIds.size == 20 }
        assertEquals(20, progress.recentlyViewedItemIds.size)
        // Most recent should be at the start
        assertEquals("playlist:playlist_25", progress.recentlyViewedItemIds.first())
        assertEquals("playlist:playlist_6", progress.recentlyViewedItemIds.last())
    }
}

