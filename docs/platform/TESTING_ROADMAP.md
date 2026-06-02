# Testing Roadmap

> Created: 2026-05-31
> Current state: 145 unit tests, 0 Compose UI tests, 0 navigation tests, 0 integration tests

---

## 1. Current Test Coverage

| Layer | Tests | Status |
|-------|-------|--------|
| **Algorithm** (SM-2) | 21 | ✅ Complete |
| **Repository** (search, filter, content) | 58 | ✅ Good |
| **ViewModel** (logic, state transitions) | 27 | ✅ Good |
| **Data persistence** | 10 | ✅ Good |
| **Data source** (JSON parsing) | 3 | ✅ Good |
| **Compose UI** (screen rendering) | 0 | ❌ Missing |
| **Navigation** (route resolution) | 0 | ❌ Missing |
| **Integration** (end-to-end flows) | 0 | ❌ Missing |

---

## 2. Compose UI Tests

### 2.1 Why They Matter

Compose UI tests catch bugs that unit tests can't:
- Layout issues (overlapping text, clipped images, wrong padding)
- Conditional rendering (e.g. `ReviewScreen` showing loading forever because `isSessionActive` wasn't set)
- State-driven UI (buttons disabled/enabled, correct content shown/hidden)
- Accessibility (content descriptions, tap targets)

### 2.2 Technology

**Compose Multiplatform testing** uses `createComposeRule()` (analogous to Android's `ComposeContentTestRule`), which lets you:
- Set composable content
- Assert node properties (text, visibility, clickability)
- Perform actions (click, type, scroll)

```kotlin
// Example setup
@get:Rule
val composeTestRule = createComposeRule()

composeTestRule.setContent {
    ReviewPagerCard(dueCount = 3, onStartReview = {})
}

// Assertions
composeTestRule.onNodeWithText("3 reviews due").assertIsDisplayed()
composeTestRule.onNodeWithContentDescription("School Icon").assertIsDisplayed()
```

### 2.3 Priority: Phase 1 — Core Screens

| Composable | What to Test | Effort |
|------------|-------------|--------|
| `HomePager` | Shows correct number of pages (2 vs 3), pager dots reflect count | 3 hrs |
| `ReviewPagerCard` | Displays count, description text, tap triggers callback | 1 hr |
| `DailyQuestionPagerCard` | Shows question text, "Tap to answer" CTA | 1 hr |
| `StoreHeroPagerCard` | Shows image, title, subtitle, gradient overlay | 1 hr |
| `ReviewBanner` (legacy, can be removed) | — | 0 hr |
| `ReviewScreen` | Loading → question → explanation → next → summary flow | 4 hrs |
| `ReviewSummaryScreen` | Shows correct count, accuracy %, reschedule breakdown | 1 hr |

**Total effort: ~12 hours**

### 2.4 Priority: Phase 2 — Complex Screens

| Composable | What to Test | Effort |
|------------|-------------|--------|
| `DiscoverScreen` | Search bar filters, channel chips, playlist chips, sort dropdown, video list | 4 hrs |
| `CourseDetailsScreen` | Course header, progress bar, episode list with thumbnails, completed checkmarks | 3 hrs |
| `VideoPlayerScreen` | Thumbnail image, play button, quiz CTA, related videos | 2 hrs |
| `QuizScreen` | Question flow, option selection, submit/next, explanation display | 3 hrs |
| `HomeScreen` | Full scroll (pager, recently viewed, playlists, popular, latest, recommended) | 4 hrs |
| `StoreScreen` | Hero, channel carousel, Good Store carousel | 2 hrs |

**Total effort: ~18 hours**

### 2.5 Implementation Notes

- Tests should go in `composeApp/src/commonTest/kotlin/.../ui/` alongside their source files
- Use the existing `TestData` objects for fixtures
- Mock all `ViewModel`s — UI tests verify rendering, not business logic
- Use `remember { mutableStateOf(...) }` for test-controlled state instead of real ViewModels

---

## 3. Navigation Tests

### 3.1 Why They Matter

Navigation bugs are subtle and hard to reproduce manually:
- Route serialization/deserialization failures (e.g. `CourseDetailsRoute` with special characters)
- Missing route registration (new screen added but not in `NavHost`)
- Back stack corruption (wrong screen shown after `popBackStack`)
- Deep link failures (app crashes when launched with a URL)

### 3.2 Technology

Navigation Compose provides `TestNavHostController` for testing:

```kotlin
val navController = TestNavHostController(context)
composeTestRule.setContent {
    NavHost(navController = navController, startDestination = MainTabsRoute) {
        // Same routes as App.kt
    }
}

// Assert navigation happened
navController.navigate(VideoPlayerRoute("abc123"))
assertEquals("VideoPlayerRoute(videoId=abc123)", navController.currentBackStackEntry?.destination?.route)
```

### 3.3 Priority Tests

| Scenario | What to Verify | Effort |
|----------|---------------|--------|
| **Route serialization** | All `@Serializable` routes encode/decode correctly | 2 hrs |
| **Tab navigation** | Switching between Home → Discover → Progress → Store → Settings works | 1 hr |
| **Deep navigation** | Home → Course → Video → Quiz → back returns to Video | 2 hrs |
| **Review flow** | Home → Review (banner tap) → Summary → back returns to Home | 1 hr |
| **Cross-tab navigation** | Home channel card tap → Discover tab with channel filter applied | 1 hr |

**Total effort: ~7 hours**

### 3.4 Implementation Notes

- Tests should verify *that* navigation happened, not *what* the screen looks like
- Use `TestNavHostController` to inspect the back stack
- Test both forward navigation (`navigate`) and backward (`popBackStack`)

---

## 4. Integration Tests

### 4.1 Why They Matter

Integration tests verify the full pipeline works end-to-end. These are the tests that catch the "bootstrap bug" we hit (completing a video didn't create review states):

| Bug We Hit | How an Integration Test Would Catch It |
|------------|--------------------------------------|
| Review states not created on video completion | Test: complete video → advance day → check `getDueReviews` returns questions |
| Review screen stuck on loading | Test: start review session → assert question loads within timeout |
| Summary shows all zeros | Test: answer questions → finish → assert summary has non-zero counts |

### 4.2 Technology

Integration tests use **real components wired together** but with **fake external dependencies**:

```kotlin
// Real: UserRepositoryImpl + ContentRepositoryImpl + SpacedRepetitionAlgorithm
// Fake: FakeJsonDataSource (bundled test data)
// Fake: createTestDataStore (in-memory DataStore)

val dataStore = createTestDataStore("integration_test", testScope)
val dataSource = TestJsonDataSource() // Returns known test data
val userRepository = UserRepositoryImpl(dataSource, dataStore, testDispatcher)
val contentRepository = ContentRepositoryImpl(dataSource, testDispatcher)

// Now test the full pipeline
```

### 4.3 Priority Integration Flows

| Flow | Steps | What to Verify | Effort |
|------|-------|---------------|--------|
| **Complete Video → Review Created** | 1. Complete video with questions 2. Advance day 3. Check due reviews | Review states exist, due date = tomorrow, question content matches | 3 hrs |
| **Full Review Session** | 1. Complete video 2. Advance day 3. Start session 4. Answer Q1 correct 5. Answer Q2 wrong 6. Finish | Session results: 50% accuracy, 1 rescheduled to week, 1 to tomorrow | 4 hrs |
| **Multi-Day Review Cycle** | 1. Complete video 2. Answer review correct 3. Advance 3 days 4. Check due 5. Answer wrong 6. Advance 1 day 7. Check due | Intervals grow correctly (1→3→7), reset on wrong answer | 4 hrs |
| **Course Completion** | 1. Complete all videos in a playlist 2. Check `completed_course_ids` | Playlist marked as completed | 2 hrs |
| **Channel Discovery → Quiz** | 1. Search for channel name 2. Navigate to video 3. Take quiz 4. Pass 5. Verify completion | Video marked completed, course progress updated, review states created | 4 hrs |

**Total effort: ~17 hours**

### 4.4 Implementation Notes

- Tests should go in a separate `IntegrationTest` class (not mixed with unit tests)
- Use `runTest` with `UnconfinedTestDispatcher` for deterministic async behavior
- Keep test data small (1 channel, 2 playlists, 3 videos, 6 questions) — don't use the full 5K video catalog
- Integration tests are slower than unit tests — expect ~1-2 seconds per test

---

## 5. Recommended Implementation Order

### Sprint 1: Compose UI Core (12 hours)
1. `ReviewPagerCard` — simplest component, good first test
2. `HomePager` — verifies dynamic page count
3. `ReviewScreen` — catches the most bugs in the review flow
4. `ReviewSummaryScreen` — validates summary rendering

**Impact:** Catches the "session ended after first answer" bug immediately.

### Sprint 2: Integration Core (11 hours)
1. **Complete Video → Review Created** — catches the "bootstrap bug"
2. **Full Review Session** — catches the "summary shows zeros" bug
3. Navigation tests (route serialization + tab navigation)

**Impact:** Catches the two biggest bugs we've hit in the review feature.

### Sprint 3: Compose UI Complex (18 hours)
1. `DiscoverScreen` — most complex screen, highest value
2. `CourseDetailsScreen` — second most complex
3. `QuizScreen` — quiz flow verification
4. Remaining screens

### Sprint 4: Navigation + Integration Edge Cases (9 hours)
1. Deep navigation flows
2. Cross-tab navigation
3. Multi-day review cycle
4. Channel discovery → quiz flow

---

## 6. Long-Term Maintenance

| Practice | Frequency |
|----------|-----------|
| **Run full test suite on every PR** | Every PR (CI) |
| **Add UI test for new composables** | As they're created |
| **Add integration test for new flows** | As they're created |
| **Review test coverage** | Monthly |
| **Flaky test investigation** | As they appear |

### CI Pipeline Recommendation

```
┌─────────────────┐
│   Lint / KtLint │ ── 30s
├─────────────────┤
│   Unit Tests    │ ── 5s (145 tests)
├─────────────────┤
│   UI Tests      │ ── 60s (estimated 50 tests)
├─────────────────┤
│   Integration   │ ── 120s (estimated 15 tests)
├─────────────────┤
│   Build APK     │ ── 120s
└─────────────────┘
Total: ~5.5 minutes
```

---

## 7. Estimated Totals

| Category | Tests | Effort |
|----------|-------|--------|
| Compose UI (Phase 1) | ~25 | 12 hrs |
| Compose UI (Phase 2) | ~35 | 18 hrs |
| Navigation | ~10 | 7 hrs |
| Integration | ~15 | 17 hrs |
| **Total** | **~85** | **54 hrs** |

This would bring total test count from 145 to **~230 tests** and increase coverage from "unit-only" to "full stack."
