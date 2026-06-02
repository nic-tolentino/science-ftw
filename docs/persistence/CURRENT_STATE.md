# Current Persistence & Memory State Analysis

> Last reviewed: 2026-05-31
> Scope: All data currently stored, cached, or computed in the app.

---

## 1. What Gets Persisted (DataStore)

**Location**: `academy.preferences_pb` (Android) / `academy.preferences_pb` (iOS)  
**Technology**: `DataStore<Preferences>` — Kotlin Multiplatform, protobuf-backed key-value store.

### 1.1 User Progress & State

| Key | Type | Default | Description | Stored As |
|-----|------|---------|-------------|-----------|
| `completed_video_ids` | `Set<String>` | `emptySet()` | YouTube IDs of watched videos. Used to show checkmarks, compute progress bars, detect completed courses. | `stringSetPreferencesKey` |
| `completed_course_ids` | `Set<String>` | `emptySet()` | Playlist IDs where all videos are completed. Computed on-the-fly in `markVideoCompleted()`. | `stringSetPreferencesKey` |
| `streak_count` | `Int` | `6` | Current day streak. Incremented via `updateStreak()`. | `intPreferencesKey` |
| `last_active_date` | `String?` | `null` | Last date the user was active (YYYY-MM-DD). Used for streak calculation. | `stringPreferencesKey` |

### 1.2 User Preferences

| Key | Type | Default | Description | Stored As |
|-----|------|---------|-------------|-----------|
| `dark_mode_enabled` | `Boolean` | `false` | Whether dark mode is enabled. | `booleanPreferencesKey` |
| `push_notifications_enabled` | `Boolean` | `true` | Whether push notifications are enabled. | `booleanPreferencesKey` |
| `interests_selected` | `Set<String>` | `{"Astronomy & Space", "Biology & Chemistry"}` | User's selected topic interests. Used for home screen personalization. | `stringSetPreferencesKey` |

### What's NOT Persisted (But Should Be)

- **Search history** — no local search history is stored
- **Last-watched video** per playlist — no resume position
- **Quiz results** — scores, attempts, time spent are lost on app restart
- **Downloaded/offline state** — no tracking of which videos are available offline
- **Video playback position** — no bookmark/resume timestamp
- **Watch history** — only completed videos are tracked, not partially-watched
- **Bookmarks/saved videos** — no "watch later" or favorites feature yet
- **Sort/filter preferences** — Discover tab resets to defaults on restart

---

## 2. What's In Memory Only (Volatile)

### 2.1 Content Catalog — Loaded at App Launch

| Data | Source | Size | Lifecycle | Notes |
|------|--------|------|-----------|-------|
| **Channels** | `channels.json` (4 entries) | ~300 bytes | `StateFlow` in `ScienceRepository`, lives as long as app process | Loaded once in `init { loadAllData() }`, cached as `Resource.Success` |
| **Playlists** | `playlists_{channel}.json` (221 total) | ~100 KB | `StateFlow` in `ScienceRepository` | Loaded once at launch, cached in-memory |
| **Videos** | `videos_{channel}.json` (5,191 total) | ~18 MB | `StateFlow` in `ScienceRepository` | Loaded once at launch, cached in-memory |
| **Questions** | Embedded in video objects | ~294 questions across 122 videos | Same as videos | Part of `Video.questions` list |

### 2.2 Derived/Computed State

| State | How Computed | Lifetime | Notes |
|-------|-------------|----------|-------|
| **Search results** | Filtered/sorted from in-memory `videosState` | Per-query, discarded when query changes | No caching of search results |
| **Channel playlists** | Filtered from `playlistsState` by channel | Per-request, not cached | Computed in `DiscoverViewModel.channelPlaylists` |
| **Matching playlists** | Filtered playlists based on current search params | Per-search, discarded on next search | Computed in `DiscoverViewModel.buildPlaylistModels()` |
| **Video progress per playlist** | Intersection of `completedVideoIds` × playlist `videoIds` | Computed on each UI recomposition | Done in `CourseDetailsViewModel` via `combine(userProgress, getVideosForPlaylist)` |
| **Course completion %** | Computed from `completedVideoIds` and playlist `videoIds` | Per-composition | Computed in `CourseDetailsScreen` |
| **User rating (computed)** | `(likeCount / viewCount * 100).coerceIn(0, 5)` | Per-video, on-demand | Computed property on `Video` model |
| **Playlist aggregates** | `totalViews`, `totalDuration`, `latestDate`, `videoCount` | Per-search, discarded | Computed in `DiscoverViewModel.buildPlaylistModels()` |

### 2.3 ViewModel State

| ViewModel | State | Notes |
|-----------|-------|-------|
| `HomeViewModel` | Combines 5 flows into a single `HomeUiState` | State recomputes on any underlying flow change |
| `DiscoverViewModel` | Query, channel filter, playlist filter, sort order | All in `MutableStateFlow`s, lost on process death |
| `CourseDetailsViewModel` | Playlist, videos, completed IDs, channel title | Recomputes from repository flows |
| `VideoPlayerViewModel` | Current video, completion status | Lost on screen dismissal |
| `QuizViewModel` | Current question, selected answer, score, quiz state | Entire quiz state is in-memory |
| `YouViewModel` | Dark mode, push notifications, interests | Syncs to DataStore but state is in-memory |
| `ProgressViewModel` | User progress | Reads directly from `repository.userProgress` StateFlow |

---

## 3. Current Data Flow Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Bundled JSON Files                    │
│  channels.json · videos_{id}.json · playlists_{id}.json  │
│  (4 files · ~18 MB total · 5,191 videos · 221 playlists) │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   JsonDataSource     │ ← Reads JSON at launch, caches in-memory
              │   (commonMain)       │ ← Lazy per-channel loading
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  ScienceRepository   │ ← 3 MutableStateFlows: channels, playlists, videos
              │  (in-memory cache)   │ ← Filters/sorts/searches in-memory
              │                      │ ← Reads/writes DataStore for user state
              └──────┬───────┬───────┘
                     │       │
                     │       ▼
                     │  ┌─────────────┐
                     │  │  DataStore  │ ← Preferences key-value store
                     │  │  (SQLite)   │ ← 7 keys: completed, streak, settings, interests
                     │  └─────────────┘
                     │
                     ▼
              ┌──────────────┐
              │  ViewModels  │ ← Combine repository flows into UI state
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │    Compose   │ ← UI renders from StateFlow
              │    Screens   │
              └──────────────┘
```

---

## 4. Performance Characteristics

### Memory Footprint at Launch

| Component | Approx. Size | Notes |
|-----------|-------------|-------|
| Videos (5,191 objects) | ~8-12 MB | Each `Video` has 17 fields + nested `Question`/`ChapterSegment` lists |
| Playlists (221 objects) | ~200 KB | Each `Playlist` has 6 fields + `videoIds` list |
| Channels (6 objects) | ~5 KB | Small, negligible |
| DataStore preferences | ~5 KB | 7 keys, very small |
| **Total estimated** | **~10-15 MB** | Well within Android/iOS memory budgets |

### Load Time

| Step | Duration | Notes |
|------|----------|-------|
| JSON parsing (18 MB) | ~200-500 ms | Single-threaded `json.decodeFromString` |
| StateFlow initialization | ~50 ms | 3 `MutableStateFlow` emissions |
| DataStore read | ~50-100 ms | Async, doesn't block UI |
| **Total cold start** | **~300-650 ms** | Data load is not the bottleneck for cold start |

---

## 5. Identified Gaps for Local Storage Planning

### 5.1 User Data Gaps

| Gap | Impact | Priority |
|-----|--------|----------|
| No watch history (only completed) | Can't show "continue watching" or resume mid-video | High |
| No video playback position | User loses their place when leaving a video | High |
| No quiz history/scores | Can't show progress on quiz mastery | Medium |
| No search history | Can't show recent searches or trending | Low |
| No sort/filter persistence | Discover tab resets on each visit | Low |

### 5.2 Content Data Gaps

| Gap | Impact | Priority |
|-----|--------|----------|
| No offline/download tracking | Can't support offline mode | Phase 2 |
| No transcript data | Can't support transcript search or summaries | Phase 2 |
| No video thumbnail caching | Thumbnails re-download every time | Medium |
| No CDN/remote content updates | Content is frozen at build time | High |

### 5.3 Architecture Considerations

- **DataStore is fine for current user state** — 7 keys is well within its capabilities. Adding watch history and quiz results would increase key count but could still work with DataStore.
- **In-memory content catalog is efficient** for 5K videos (~10-15 MB), but will need a database (SQLDelight/Room) when:
  - Adding transcript search (FTS5) — would add ~120 MB of text data
  - Scaling beyond ~10K videos
  - Need relational queries (e.g., "show me all videos I started but didn't finish")
  - Need offline content management

---

## 6. DataStore Key Reference

All keys are defined in `ScienceRepositoryImpl.Companion`:

```kotlin
KEY_COMPLETED_VIDEO_IDS    → stringSetPreferencesKey("completed_video_ids")
KEY_COMPLETED_COURSE_IDS   → stringSetPreferencesKey("completed_course_ids")
KEY_STREAK_COUNT           → intPreferencesKey("streak_count")
KEY_LAST_ACTIVE_DATE       → stringPreferencesKey("last_active_date")
KEY_DARK_MODE              → booleanPreferencesKey("dark_mode_enabled")
KEY_PUSH_NOTIFICATIONS     → booleanPreferencesKey("push_notifications_enabled")
KEY_INTERESTS              → stringSetPreferencesKey("interests_selected")
```

DataStore file: `academy.preferences_pb` (Android: `filesDir`, iOS: `NSCachesDirectory`)
