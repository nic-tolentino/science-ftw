# Knowledge Reinforcement System — Spaced Repetition Design

> Created: 2026-05-31
> Status: Draft — design document
> Related: [QUIZ_AUTHORING_STRATEGY.md](QUIZ_AUTHORING_STRATEGY.md)

---

## 1. The Problem

Currently, a user watches a video, passes a 3-question micro-quiz, and the video is marked "completed" forever. There is **no mechanism to revisit or reinforce** that knowledge over time.

Without reinforcement, the forgetting curve kicks in:
- **After 1 day**: User retains ~60% of what they learned
- **After 7 days**: ~35% retained
- **After 30 days**: ~20% retained

Spaced repetition solves this by scheduling review sessions at increasing intervals, just before the user is about to forget.

---

## 2. Core Design Principles

1. **Frictionless** — Reinforcement quizzes appear naturally in the user's existing flow. No separate "review mode" screen.
2. **Offline-first** — All scheduling, scoring, and question selection happens on-device. No network needed.
3. **Lightweight** — Reviews are 3-question micro-quizzes (same format as the proof-of-knowledge quiz), taking ~30 seconds.
4. **Respectful of time** — Never more than one review prompt per session unless the user opts into a "review session."
5. **Zero-cost** — No LLM API calls at runtime. All questions come from bundled JSON.

---

## 3. The Algorithm

We use a simplified **SM-2 algorithm** (the algorithm behind Anki and SuperMemo), adapted for video-based learning.

### 3.1 Per-Question State

Each question the user has ever answered tracks:

| Field | Type | Description |
|-------|------|-------------|
| `questionId` | String | Unique identifier (e.g., `q_cc_biology_0_0`) |
| `videoId` | String | YouTube ID of the source video |
| `easeFactor` | Double | Difficulty multiplier. Starts at 2.5. Range: 1.3–3.0 |
| `intervalDays` | Int | Days until next review. Starts at 1. |
| `repetitions` | Int | Number of successful answers. Starts at 0. |
| `nextReviewDate` | String (YYYY-MM-DD) | When this question should next be reviewed |
| `lastScore` | Double | Score of last attempt (0.0–1.0) |
| `lastAnsweredDate` | String (YYYY-MM-DD) | When last reviewed |

### 3.2 SM-2 Scoring

After the user answers a question, the algorithm updates:

```kotlin
fun updateReviewState(
    current: QuestionReviewState,
    quality: Double  // 0.0 (wrong) to 1.0 (correct)
): QuestionReviewState {

    // Ease factor adjustment
    val newEase = (current.easeFactor + (0.1 - (1.0 - quality) * (0.1 - quality * 0.5)))
        .coerceIn(1.3, 3.0)

    val newRepetitions = if (quality >= 0.7) {
        current.repetitions + 1
    } else {
        0  // Reset repetitions on poor answer
    }

    // Interval calculation
    val newInterval = when {
        newRepetitions == 0 -> 1       // Review tomorrow
        newRepetitions == 1 -> 3       // Review in 3 days
        newRepetitions == 2 -> 7       // Review in 1 week
        else -> (current.intervalDays * newEase).toInt().coerceAtLeast(1)
    }

    // Next review date
    val nextDate = LocalDate.now().plusDays(newInterval.toLong())

    return current.copy(
        easeFactor = newEase,
        intervalDays = newInterval,
        repetitions = newRepetitions,
        nextReviewDate = nextDate.toString(),
        lastScore = quality,
        lastAnsweredDate = LocalDate.now().toString()
    )
}
```

### 3.3 The Forgetting Curve Model

The SM-2 interval calculation implicitly models the forgetting curve:

```
Memory Strength
    ↑
    │  ╭── review (correct)
    │  │     ╭── review (correct)
    │  │     │      ╭── review (correct)
    │  │     │      │      ╭── review (correct)
    │  ╰─────╯      │      │
    │               ╰──────╯
    │                      ╰─────── longer gaps
    └─────────────────────────────→ Time
      D1  D3    D7     D18    D47
```

A wrong answer resets the curve — the question returns to a 1-day interval.

---

## 4. Data Sources

### 4.1 Input: Completed Videos

The system draws review questions from videos the user has completed:

```kotlin
val completedVideoIds = dataStore[KEY_COMPLETED_VIDEO_IDS]  // Set<String>
val completedVideos = allVideos.filter { it.id in completedVideoIds }
val completedQuestions = completedVideos.flatMap { it.questions }
```

### 4.2 Question Pool

Only videos with bundled questions can generate reviews. Currently:
- 122 videos with questions (294 total questions)
- 1,903 CrashCourse videos need questions generated (per [QUIZ_AUTHORING_STRATEGY.md](QUIZ_AUTHORING_STRATEGY.md))

**Priority**: Generate questions for the most-watched videos first. A user can't review what they haven't learned.

---

## 5. What Gets Persisted

### 5.1 New DataStore Keys

| Key | Type | Description |
|-----|------|-------------|
| `question_review_states` | `String` (JSON) | Serialized `Map<String, QuestionReviewState>`. Stored as a single JSON string to work around DataStore's lack of nested maps. |
| `daily_review_count` | `Int` | Number of reviews completed today (resets daily). |
| `last_review_date` | `String` | Date of last review session (YYYY-MM-DD). Used to detect new day. |

### 5.2 JSON Schema (stored in `question_review_states`)

```json
{
  "q_cc_biology_0_0": {
    "questionId": "q_cc_biology_0_0",
    "videoId": "tZE_fQFK8EY",
    "easeFactor": 2.5,
    "intervalDays": 1,
    "repetitions": 0,
    "nextReviewDate": "2026-06-01",
    "lastScore": 1.0,
    "lastAnsweredDate": "2026-05-30"
  }
}
```

Estimated size: ~150 bytes per question. For 294 questions = ~44 KB. For a fully-questioned catalog (5,191 videos × 3 questions = ~15,573 questions) = ~2.3 MB. Well within DataStore limits.

---

## 6. Review Session Flow

### 6.1 Daily Review Prompt

On app launch (or when the Home screen loads), check if there are due reviews:

```kotlin
fun getDueReviews(): List<QuestionReviewState> {
    val today = LocalDate.now()
    return allQuestionStates.values.filter {
        LocalDate.parse(it.nextReviewDate) <= today
    }
}
```

### 6.2 Where It Appears

**Option A: Home Screen Banner** (recommended for Phase 1)

```
┌──────────────────────────────────────────┐
│  🔔  7 reviews due                       │
│  You have 7 questions ready for review   │
│  [ Review Now → ]            [ Dismiss ] │
└──────────────────────────────────────────┘
```

**Option B: Dedicated Tab** (Phase 2)

A 5th tab between Progress and You:
```
[ Home ] [ Explore ] [ Review ] [ Progress ] [ You ]
```

### 6.3 Review Quiz Screen

Same as the existing `QuizScreen` but with context:

```
┌──────────────────────────────────────────┐
│  Review · Biology                        │
│  "What is the powerhouse of the cell?"   │
│                                          │
│  ○ Nucleus                               │
│  ● Mitochondria ✓                        │
│  ○ Ribosome                              │
│                                          │
│  [Next →]                                │
└──────────────────────────────────────────┘
```

Differences from proof-of-knowledge quiz:
- No "pass/fail" threshold — every answer updates the review state
- Questions come from **all** completed videos, not just the current one
- The explanation always shows (correct and incorrect), reinforcing learning

### 6.4 Post-Review Summary

```
┌──────────────────────────────────────────┐
│  Review Complete 🎉                      │
│                                          │
│  5 of 7 correct (71%)                    │
│                                          │
│  2 questions rescheduled for tomorrow    │
│  3 questions rescheduled for next week   │
│  2 questions rescheduled for next month  │
│                                          │
│  [ Continue → ]                          │
└──────────────────────────────────────────┘
```

---

## 7. Integration Points

### 7.1 Repository Layer

New methods on `ScienceRepository`:

```kotlin
interface SpacedRepetitionEngine {
    /** Get questions due for review today. */
    fun getDueReviews(): Flow<List<QuestionReviewState>>

    /** Record an answer and compute next review date. */
    suspend fun recordAnswer(questionId: String, quality: Double)

    /** Get review stats: total reviewed, accuracy, streak. */
    suspend fun getReviewStats(): ReviewStats

    /** Get upcoming review schedule (next 7 days). */
    suspend fun getUpcomingReviews(daysAhead: Int = 7): Map<LocalDate, Int>
}
```

### 7.2 ViewModel

```kotlin
class ReviewViewModel(
    private val repository: ScienceRepository,
    private val spacedRepetition: SpacedRepetitionEngine
) : BaseViewModel<ReviewUiState, ReviewEvent>(ReviewUiState()) {

    fun startReviewSession() { ... }
    fun submitAnswer(questionId: String, isCorrect: Boolean) { ... }
    fun nextQuestion() { ... }
}
```

### 7.3 DataStore Integration

```kotlin
private val KEY_QUESTION_STATES = stringPreferencesKey("question_review_states")

suspend fun saveQuestionState(states: Map<String, QuestionReviewState>) {
    val json = json.encodeToString(states)
    dataStore.edit { it[KEY_QUESTION_STATES] = json }
}

suspend fun loadQuestionStates(): Map<String, QuestionReviewState> {
    val json = dataStore.data.map { it[KEY_QUESTION_STATES] ?: "{}" }.first()
    return json.decodeFromString()
}
```

---

## 8. Bootstrapping: Initial Review State

When a user first enables spaced repetition (or when the feature launches), existing completed videos need initial review states:

```kotlin
suspend fun initializeReviewStates() {
    val completedVideos = repository.getCompletedVideos()
    val questions = completedVideos.flatMap { it.questions }

    val initialStates = questions.associate { q ->
        q.id to QuestionReviewState(
            questionId = q.id,
            videoId = q.videoId,
            easeFactor = 2.5,
            intervalDays = 1,
            repetitions = 0,
            nextReviewDate = LocalDate.now().toString(),  // All due immediately
            lastScore = 0.0,
            lastAnsweredDate = ""
        )
    }

    saveQuestionStates(initialStates)
}
```

This means on first launch, the user may have many due reviews. We should cap the daily session:

```kotlin
val maxReviewsPerDay = 15
val dueReviews = getDueReviews().take(maxReviewsPerDay)
```

---

## 9. UI/UX Considerations

### 9.1 Streak Integration

The spaced repetition system should integrate with the existing streak:

- Completing a daily review session extends the streak
- Missing a review day doesn't break the streak (reviews are bonus, not required)
- A separate "review streak" tracks consecutive days of completing reviews

### 9.2 Difficulty Feedback

The ease factor implicitly measures question difficulty:
- Low ease (1.3–1.8): User consistently gets this question wrong
- Medium ease (1.9–2.4): User sometimes gets it wrong
- High ease (2.5–3.0): User consistently gets it right

This data could power future features:
- Flag poorly-worded questions for review (low ease across many users)
- Surface "weak areas" to the user ("You struggle with cell biology questions")

### 9.3 Notifications

Optional push notifications for due reviews:
- "You have 5 reviews due today" — shown at the user's preferred time
- Configurable: daily, every 2 days, or off

---

## 10. Phased Implementation Plan

### Phase 1: Foundation (2-3 weeks)
- [ ] Add `QuestionReviewState` data class
- [ ] Add DataStore keys and serialization
- [ ] Implement SM-2 algorithm
- [ ] Add `SpacedRepetitionEngine` to repository
- [ ] Home screen banner for due reviews
- [ ] Review quiz screen (reuse existing QuizScreen with review context)
- [ ] Post-review summary screen

### Phase 2: Polish (1-2 weeks)
- [ ] Review tab in bottom navigation
- [ ] Review streak tracking
- [ ] Upcoming review calendar (see what's due this week)
- [ ] Review stats screen (accuracy over time, strongest/weakest topics)
- [ ] Push notifications for due reviews

### Phase 3: Intelligence (future)
- [ ] Cross-user difficulty analytics (if cloud backend exists)
- [ ] Adaptive question generation (LLM generates questions for topics the user struggles with)
- [ ] "Weak area" identification and targeted review sessions
- [ ] Integration with video chapters (review questions link to specific timestamps)

---

## 11. Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| Bundled questions in videos.json | ✅ Exists (294 questions, 122 videos) | Need more questions for full coverage |
| Completed video tracking | ✅ Exists (`completed_video_ids` in DataStore) | Just needs to feed into review engine |
| DataStore | ✅ Exists | Need to add 3 new keys |
| Quiz screen UI | ✅ Exists (`QuizScreen.kt`, `QuizViewModel.kt`) | Needs review mode variant |
| SM-2 algorithm | ❌ Not implemented | New code, ~50 lines |
| `kotlinx-datetime` | ✅ Already a dependency | For date arithmetic |

---

## 12. Comparison to Existing Solutions

| System | Approach | Our Fit |
|--------|----------|---------|
| **Anki** | User creates flashcards, reviews on schedule | We auto-generate questions from video content — no manual card creation |
| **Duolingo** | Spaced repetition built into skill trees | We don't have skill trees, but videos map naturally to topics |
| **SuperMemo** | Complex algorithm with many factors | We use simplified SM-2 — sufficient for our use case |
| **Leitner System** | Physical boxes, manual card movement | Digital equivalent, but algorithmic (no manual sorting) |

Our key differentiator: **zero authoring effort**. Questions come from bundled video quizzes. The user just watches videos and takes the built-in proof-of-knowledge quiz — spaced repetition happens automatically on top of that.
