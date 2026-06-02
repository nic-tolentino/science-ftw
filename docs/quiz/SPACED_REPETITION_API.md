# Spaced Repetition System — API Reference

> Last updated: 2026-05-31
> Package: `com.complexly.academy.spacedrepetition`

---

## 1. Overview

The spaced repetition system uses the **SM-2 algorithm** (the same algorithm behind Anki and SuperMemo) to schedule review questions at increasing intervals, just before the user is about to forget the material.

**Key properties:**
- **Offline-first** — all scheduling, scoring, and state management happens on-device
- **Zero LLM cost** — all questions come from bundled JSON
- **Frictionless** — reviews appear naturally as a banner on the Home screen
- **Lightweight** — 3-question micro-quizzes, same format as proof-of-knowledge

---

## 2. Data Model

### 2.1 QuestionReviewState

```kotlin
data class QuestionReviewState(
    val questionId: String,       // e.g. "q_cc_biology_0_0"
    val videoId: String,          // YouTube ID of the source video
    val easeFactor: Double = 2.5, // Range: 1.3–3.0
    val intervalDays: Int = 1,    // Days until next review
    val repetitions: Int = 0,     // Consecutive correct answers
    val nextReviewDate: String = "",  // YYYY-MM-DD
    val lastScore: Double = 0.0,      // 0.0–1.0
    val lastAnsweredDate: String = "" // YYYY-MM-DD
)
```

**Lifecycle:**
1. **Created** when a user first answers a question (via `recordAnswer`)
2. **Updated** each time the user reviews the question
3. **Persisted** in DataStore as a JSON string map

**Key methods:**
- `isDue(onDate: LocalDate = today()): Boolean` — whether the question is due for review

### 2.2 ReviewStats

```kotlin
data class ReviewStats(
    val totalQuestions: Int,       // Total questions with review states
    val reviewedQuestions: Int,    // Questions that have been answered at least once
    val dueToday: Int,             // Questions due today
    val dueThisWeek: Int,          // Questions due within 7 days
    val overallAccuracy: Double,   // 0.0–1.0
    val currentStreak: Int,        // Consecutive days with reviews
    val longestStreak: Int
)
```

### 2.3 ReviewSessionResult

```kotlin
data class ReviewSessionResult(
    val totalAnswered: Int,         // Questions answered in this session
    val correctCount: Int,          // Correct answers
    val rescheduledTomorrow: Int,   // Questions rescheduled for tomorrow
    val rescheduledThisWeek: Int,   // Questions rescheduled within 7 days
    val rescheduledLater: Int       // Questions rescheduled beyond 7 days
) {
    val accuracy: Double  // Computed: correctCount / totalAnswered
}
```

---

## 3. Algorithm

### 3.1 SM-2 Update Rule

The algorithm is implemented in `SpacedRepetitionAlgorithm.updateState()`:

```kotlin
fun updateState(
    current: QuestionReviewState,
    quality: Double  // 0.0 = wrong, 1.0 = correct
): QuestionReviewState
```

**Steps:**
1. **Ease factor adjustment:** `newEase = easeFactor + (0.1 - (1 - quality) * (0.08 - quality * 0.02))`, clamped to [1.3, 3.0]
2. **Repetitions:** Increment if quality ≥ 0.7, reset to 0 otherwise
3. **Interval calculation:**
   - rep = 0 → 1 day (review tomorrow)
   - rep = 1 → 3 days
   - rep = 2 → 7 days
   - rep ≥ 3 → `intervalDays * easeFactor` (rounded down, minimum 1)
4. **Next review date:** `today + intervalDays`

### 3.2 Passing Threshold

Questions are considered "correct" if `quality ≥ 0.7`. This means:
- `quality = 1.0` → fully correct, maximum ease increase
- `quality = 0.7` → passing threshold, moderate ease increase
- `quality = 0.6` → fails, repetitions reset to 0, interval = 1 day
- `quality = 0.0` → completely wrong, minimum ease

### 3.3 The Forgetting Curve

```
Memory Strength
    ↑
    │  ╭── correct (interval: 3 days)
    │  │     ╭── correct (interval: 7 days)
    │  │     │      ╭── correct (interval: 18 days)
    │  │     │      │      ╭── correct (interval: 47 days)
    │  ╰─────╯      │      │
    │               ╰──────╯
    │                      ╰─────── longer gaps
    └─────────────────────────────→ Time
      D1  D3    D7     D18    D47
```

A wrong answer resets the curve back to D1.

---

## 4. UserRepository Interface

### 4.1 Methods

```kotlin
interface UserRepository {
    // ... existing methods ...

    /**
     * Records an answer to a review question and updates the spaced repetition state.
     * Persists the updated state to DataStore.
     *
     * @param questionId The question's unique identifier
     * @param videoId The YouTube ID of the source video (used for initial state creation)
     * @param quality Score from 0.0 (wrong) to 1.0 (correct)
     */
    suspend fun recordAnswer(questionId: String, videoId: String, quality: Double)

    /**
     * Returns questions that are due for review today, up to [maxCount].
     * Results are sorted by nextReviewDate (earliest first).
     */
    fun getDueReviews(maxCount: Int = 15): List<QuestionReviewState>

    /**
     * Returns aggregate statistics about review performance.
     * Computed from current review states and learning progress.
     */
    fun getReviewStats(): ReviewStats
}
```

### 4.2 DataStore Keys

| Key | Type | Description |
|-----|------|-------------|
| `question_review_states` | `String` (JSON) | Serialized `Map<String, QuestionReviewState>` |

### 4.3 JSON Schema

```json
{
  "q_cc_biology_0_0": {
    "questionId": "q_cc_biology_0_0",
    "videoId": "tZE_fQFK8EY",
    "easeFactor": 2.5,
    "intervalDays": 3,
    "repetitions": 1,
    "nextReviewDate": "2026-06-03",
    "lastScore": 1.0,
    "lastAnsweredDate": "2026-05-31"
  }
}
```

---

## 5. UI Components

### 5.1 ReviewBanner

Shown on the Home screen when there are due reviews.

```kotlin
@Composable
fun ReviewBanner(
    dueCount: Int,
    onStartReview: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Appearance:** Dark card with school icon, "X reviews due" text, and "Review" button.

### 5.2 ReviewScreen

Full-screen review session.

```kotlin
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Flow:** Question → Select answer → Submit → See explanation → Next question → Summary screen

### 5.3 ReviewViewModel

```kotlin
class ReviewViewModel(
    private val userRepository: UserRepository,
    private val contentRepository: ContentRepository
) : ViewModel() {
    val uiState: StateFlow<ReviewUiState>

    fun onSessionStarted()           // Start a review session
    fun onSessionDismissed()         // Dismiss without completing
    fun onSessionComplete()          // Dismiss after completing
    fun onSelectAnswer(index: Int)   // Select an answer option
    fun onSubmitAnswer()             // Submit the selected answer
    fun onNextQuestion()             // Move to the next question (or finish)
}
```

---

## 6. Usage Examples

### 6.1 Recording an Answer

```kotlin
// User selected the correct answer
userRepository.recordAnswer("q_cc_biology_0_0", "tZE_fQFK8EY", quality = 1.0)

// User selected a wrong answer
userRepository.recordAnswer("q_cc_biology_0_0", "tZE_fQFK8EY", quality = 0.0)

// Partial credit (e.g., multiple choice with partial matching)
userRepository.recordAnswer("q_cc_biology_0_0", "tZE_fQFK8EY", quality = 0.8)
```

### 6.2 Getting Due Reviews

```kotlin
val dueReviews = userRepository.getDueReviews(maxCount = 15)
// Returns up to 15 questions due today, sorted by nextReviewDate
```

### 6.3 Checking Review Stats

```kotlin
val stats = userRepository.getReviewStats()
println("Due today: ${stats.dueToday}")
println("Accuracy: ${stats.overallAccuracy * 100}%")
```

---

## 7. Testing

### 7.1 Algorithm Tests

Located at: `src/commonTest/kotlin/.../spacedrepetition/SpacedRepetitionAlgorithmTest.kt`

Covers:
- Initial state creation (due tomorrow)
- Correct answer progression (1→3→7→18→47 days)
- Wrong answer reset (interval → 1 day, repetitions → 0)
- Partial correctness (quality 0.7 passes, 0.69 fails)
- Ease factor bounds (1.3–3.0)
- `isDue()` logic (past, present, future, empty, malformed)

### 7.2 Repository Tests

Located at: `src/commonTest/kotlin/.../data/UserRepositorySpacedRepetitionTest.kt`

Covers:
- `recordAnswer` creates and updates review states
- `getDueReviews` filters by due date and respects maxCount
- `getReviewStats` computes accuracy and due counts
- Review states persist across DataStore edits

### 7.3 Running Tests

```bash
# All tests
./gradlew :composeApp:testDebugUnitTest

# Spaced repetition only
./gradlew :composeApp:testDebugUnitTest --tests "com.complexly.academy.spacedrepetition.*"
./gradlew :composeApp:testDebugUnitTest --tests "com.complexly.academy.data.UserRepositorySpacedRepetitionTest"
```

---

## 8. Design Decisions

### 8.1 Why SM-2?

- **Simple** — only 3 state variables per question (ease, interval, repetitions)
- **Proven** — the algorithm behind Anki and SuperMemo, used by millions
- **Lightweight** — no ML models, no cloud sync needed
- **Adaptable** — ease factor self-adjusts based on user performance

### 8.2 Why 0.7 Passing Threshold?

The SM-2 algorithm uses a 0-5 quality scale. We map binary answers to:
- Correct → 1.0
- Wrong → 0.0

The 0.7 threshold means only perfect (1.0) answers pass, but leaves room for future partial-credit features (e.g., "you selected a partially correct answer, quality = 0.5").

### 8.3 Why JSON String in DataStore?

DataStore's `Preferences` API only supports primitive types and sets. A `Map<String, QuestionReviewState>` needs to be serialized. We chose:
- **JSON string** (not proto) for simplicity and debuggability
- **Single key** (not one key per question) to avoid DataStore performance issues with many keys

### 8.4 Max 15 Reviews Per Day

- **Prevents overwhelming** users on first launch (many questions may be due)
- **Respects time** — ~30 seconds per review session
- **Configurable** — the `maxCount` parameter in `getDueReviews`

---

## 9. Future Enhancements

| Feature | Description | Effort |
|---------|-------------|--------|
| **Push notifications** | "X reviews due today" at user's preferred time | Medium |
| **Review tab** | Dedicated tab for reviews, calendar, stats | Medium |
| **Weak area detection** | Identify topics the user struggles with | Medium |
| **Adaptive question generation** | LLM generates questions for weak areas | High |
| **Cloud sync** | Sync review states across devices | High |
| **Streak tracking** | Separate "review streak" from learning streak | Low |
