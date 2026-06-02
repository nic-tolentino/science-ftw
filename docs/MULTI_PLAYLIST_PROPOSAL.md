# Proposal: Multi-Playlist Video Support

> Created: 2026-05-31
> Status: Draft — awaiting decision
> Trigger: 54 of 5,137 unique videos (1%) appear in 2+ playlists. This will grow as we add more channels and curated collections.

---

## 1. The Problem

Currently `Video.playlistId` is a single `String`:

```kotlin
@Serializable
data class Video(
    val id: String,
    val playlistId: String,  // ← assumes 1:1
    ...
)
```

But in reality:
- **54 videos** are in 2 playlists (e.g. a SciShow video appears in both "2024 Hall of Fame" AND "Cool Reptiles")
- **0 videos** are currently in 3+ playlists, but this is expected as we add more curated collections
- **Videos always belong to exactly 1 channel** (YouTube constraint)

This means our current data model silently drops or misrepresents 1% of the catalog.

---

## 2. Current Usage Sites

There are **~30 references** to `playlistId` across the codebase. Here they are grouped by concern:

### 2.1 Channel Resolution (critical — correct behavior needed)

| File | Code | What it does | Risk |
|------|------|-------------|------|
| `ScienceRepository.kt:262` | `resolveChannelFor(it.playlistId)` | Filter by channel in search | **HIGH** — currently picks first playlist's channel; if a cross-listed video has different channels, it will show for the wrong channel filter |
| `ScienceRepository.kt:266` | `resolveChannelFor(it.playlistId)` | Filter by explicit channel filter | **HIGH** — same issue |
| `VideoPlayerViewModel.kt:46` | `getChannelIdForPlaylist(v.playlistId)` | Resolve channel for "More from this channel" | **MEDIUM** — picks one channel, may show wrong "more videos" |
| `VideoPlayerViewModel.kt:58` | `playlistChannelMap[video.playlistId]` | Resolve channel for related videos | **MEDIUM** |
| `DiscoverViewModel.kt:133` | `getChannelIdForPlaylist(video.playlistId)` | Build VideoUiModel | **LOW** — cosmetic only |
| `QuizViewModel.kt:68` | `getChannelIdForPlaylist(videoRes.data.playlistId)` | Set video channel context | **LOW** |

**Cross-channel videos:** Currently none exist (a video can only be on one YouTube channel). So `resolveChannelFor()` always returns the correct channel regardless of which playlist we resolve through. But this is an invariant we should not rely on silently.

### 2.2 Playlist Lookup (correct behavior needed)

| File | Code | What it does | Risk |
|------|------|-------------|------|
| `HomeViewModel.kt:91` | `playlists.find { it.id == video.playlistId }` | Resolve playlist name for "courses in progress" | **HIGH** — only finds first playlist, hides others |
| `HomeScreen.kt:337,368,398` | `playlists.find { it.id == video.playlistId }` | Resolve playlist for channel name display | **HIGH** — same issue |
| `DiscoverViewModel.kt:134` | `playlistMap[video.playlistId]` | Display playlist title in search results | **MEDIUM** — shows one playlist, not all |
| `HomeScreen.kt:263,299` | `filter { it.playlistId == playlist.id }` | Filter videos for playlist card on Home | **HIGH** — cross-listed videos only show in first playlist |
| `CourseDetailsViewModel.kt:30` | `getVideosForPlaylist(playlistId)` | Get videos for a course page | **HIGH** — only finds videos whose `playlistId` matches |

### 2.3 Grouping & Sorting

| File | Code | What it does | Risk |
|------|------|-------------|------|
| `HomeViewModel.kt:59` | `videos.groupBy { it.playlistId }` | Group videos by playlist for Home | **HIGH** — each video only appears in one group |
| `DiscoverViewModel.kt:139` | `results.groupBy { it.playlistId }` | Group videos by playlist for playlist matching | **HIGH** — same issue |

### 2.4 Video Player — Related Videos

| File | Code | What it does | Risk |
|------|------|-------------|------|
| `VideoPlayerViewModel.kt:64` | `other.playlistId == video.playlistId` | Show "up next" from same playlist | **MEDIUM** — only shows videos from one playlist |
| `VideoPlayerScreen.kt:435` | `getChannelIdFromPlaylist(relatedVideo.playlistId)` | Display channel name for related videos | **LOW** — cosmetic |

### 2.5 Repository — Playlist Video Lookup

| File | Code | What it does | Risk |
|------|------|-------------|------|
| `ScienceRepository.kt:183` | `filter { it.playlistId == playlistId }` | Find all videos in a playlist | **HIGH** — misses cross-listed videos |

### 2.6 Search — Playlist Title Match

| File | Code | What it does | Risk |
|------|------|-------------|------|
| `ScienceRepository.kt:255` | `playlistTitleMap[video.playlistId]` | Match search query against playlist title | **MEDIUM** — only matches first playlist's title |

---

## 3. The Invariant That Matters

**A video always belongs to exactly 1 channel.** This is a YouTube constraint — each video has one `channelId` on YouTube. So we can safely resolve the channel from any of the video's playlists.

This means the channel resolution logic (group 2.1) actually works correctly today by coincidence — all playlists a video appears in share the same channel. But we should still fix it to be explicit.

---

## 4. Proposed Solution

### 4.1 Model Change

```kotlin
// Before
val playlistId: String,

// After
val playlistIds: List<String> = emptyList(),
```

In the JSON, this becomes:
```json
{
  "id": "abc123",
  "playlistIds": ["cc_biology", "cc_best_of"],
  ...
}
```

For the 99% of videos with a single playlist:
```json
{
  "id": "xyz789",
  "playlistIds": ["cc_chemistry"],
  ...
}
```

### 4.2 Migration Strategy

**Step 1: Add `playlistIds`, keep `playlistId` as computed property (backward compat)**
```kotlin
val playlistIds: List<String> = emptyList(),

@Deprecated("Use playlistIds", ReplaceWith("playlistIds.firstOrNull() ?: \"\""))
val playlistId: String
    get() = playlistIds.firstOrNull() ?: ""
```

**Step 2: Update all usage sites** — change `video.playlistId` to:
- `video.playlistIds` when doing `groupBy` or `contains` checks
- `video.playlistIds.first()` when a single value is needed (with appropriate comment)

**Step 3: Update repository** — `getVideosForPlaylist(playlistId)` should filter with:
```kotlin
filter { video -> playlistId in video.playlistIds }
```

**Step 4: Remove `playlistId` computed property** once all call sites are migrated.

### 4.3 Impact on DataStore

`completed_video_ids` stores YouTube IDs (not playlist-based), so **no DataStore migration needed**. Completed progress tracks by video, not by playlist membership.

`completed_course_ids` stores playlist IDs — also **no migration needed** since it references playlists, not videos.

### 4.4 Impact on JSON Files

Each video entry gains a `playlistIds` array instead of a single `playlistId` string. This increases JSON size slightly:
- Current: `playlistId: "cc_biology"` (30 bytes)
- After: `playlistIds: ["cc_biology"]` (34 bytes)
- For 5,191 videos: ~20 KB increase → negligible

**The pull script** (`pull_channel.py`) would need to be updated to populate `playlistIds` — currently it sets `playlistId` to the single playlist being pulled. Cross-playlist detection would require a post-processing step that scans all channel files for duplicate `youtubeId` values.

### 4.5 Code Changes Required

| Area | Files | Changes |
|------|-------|---------|
| **Model** | `Models.kt` | Add `playlistIds: List<String>`, deprecate `playlistId` |
| **Repository** | `ScienceRepository.kt` | Change all `== playlistId` to `in playlistIds`, `groupBy { it.playlistId }` to `flatMap { it.playlistIds }.distinct()` where needed |
| **Home** | `HomeViewModel.kt`, `HomeScreen.kt` | `find { it.id == video.playlistId }` → `firstOrNull { it.id in video.playlistIds }` |
| **Discover** | `DiscoverViewModel.kt` | `groupBy { it.playlistId }` → group by each playlist in `playlistIds` (video appears in multiple groups) |
| **VideoPlayer** | `VideoPlayerViewModel.kt`, `VideoPlayerScreen.kt` | Same as Home — resolve first playlist for display, or aggregate all playlists |
| **Quiz** | `QuizViewModel.kt` | `getChannelIdForPlaylist(videoRes.data.playlistId)` → `getChannelIdForPlaylist(videoRes.data.playlistIds.first())` |
| **Pull Script** | `scripts/pull_channel.py` | Add post-processing step to detect duplicate `youtubeId` across channels and merge `playlistIds` |
| **Tests** | `TestData.kt`, `SearchFilterSortTest.kt` | Update test fixtures to use `playlistIds` |

**Estimated effort:** ~25 code changes across 9 files + 1 script update.

---

## 5. Alternative: Computed Reverse Lookup

Instead of storing `playlistIds` on every video, build a `Map<youtubeId, List<playlistId>>` from playlists at load time:

```kotlin
class ScienceRepositoryImpl(...) {
    private lateinit var videoToPlaylists: Map<String, List<String>>

    private suspend fun loadAllData() {
        // ... load channels, playlists, videos ...
        // Build reverse index
        videoToPlaylists = playlists.flatMap { pl ->
            pl.videoIds.map { vid -> vid to pl.id }
        }.groupBy({ it.first }, { it.second })
    }
}
```

**Pros:** No model change, no JSON changes, no migration
**Cons:** Still needs all ~25 code changes to use the reverse map instead of `playlistId`

This approach is equally valid and may be cleaner since the playlist-to-video mapping already exists in `Playlist.videoIds`. The `playlistId` field on Video becomes redundant data.

---

## 6. Recommendation

**Use Alternative (computed reverse lookup)** — it's cleaner because:

1. `Playlist.videoIds` is already the authoritative mapping. Storing `playlistIds` on Video duplicates this data.
2. No JSON file changes needed — the 5,191 video entries don't need updating.
3. The reverse map is built once at launch (~10 ms for 221 playlists).
4. If a video is added to a new playlist, only the playlist file changes — no video file update needed.
5. When we eventually move to a database, this becomes a simple many-to-many join table.

**Migration plan:**
1. Add `videoToPlaylists: Map<String, List<String>>` to repository (built from playlists at load time)
2. Add `val Video.playlists: List<Playlist>` computed extension or helper in repository
3. Change all `video.playlistId` references to use the reverse map
4. Keep `playlistId: String` on `Video` model but mark deprecated — remove after all call sites migrated
5. Add dedup step to `pull_channel.py` so it detects cross-channel playlist membership
