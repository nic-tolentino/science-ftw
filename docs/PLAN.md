# Project Plan: Science FTW MVP - Status: Phase 1 Completed & Core Features Integrated

This document outlines the development plan and matches it against the current implementation of the Complexly Academy MVP.

## Current Project Status (May 2026)
* **Phase 1 (App Skeleton, Navigation, Mock Data)**: **100% Completed**
* **Phase 2 (Persistence & Quizzes)**: **75% Completed** (Interactive Quiz System and progress persistence are fully built using DataStore; SQLDelight database integration remains for local full-text search index).
* **Phase 3 (Gamification Layer)**: **100% Completed** (Daily streaks, completed video counting, and dynamic badge/achievement unlocking are fully implemented and persist across app launches).
* **Phase 4 (Polish & Pitch Preparation)**: **80% Completed** (Store screen / merch shelf, DFTBA & Patreon links, custom visual theme with CrashCourse/SciShow/Eons colors, and extensive test coverage are fully implemented).

---

## Core Principles
1. **Extensive Unit Testing**: All ViewModels, UseCases, and Repositories must have comprehensive unit test coverage (Achieved: 100% passing tests covering routing, VM state, sorting, and JSON ingestion).
2. **Compile-Time Safety**: Prioritize compile-time Dependency Injection (`kotlin-inject`) and type-safe Jetpack Compose Navigation.
3. **Layered Architecture**: Strict adherence to `View -> ViewModel -> Repository -> DataSource`.
4. **Documentation-Driven**: Keep plans, designs, and ADRs up to date.

---

## Phase 1: App Skeleton, Navigation & Mock Data [COMPLETED]
**Goal**: Establish a bulletproof foundation. No local database or complex network logic yet. Everything relies on mock JSON files functioning as a pseudo-API. 

1. **Initialize Project & Infrastructure** [COMPLETED]
   - Setup Kotlin Multiplatform project targeting Android (API 26+) and iOS (iOS 16+).
   - Configure Compose Multiplatform for both platforms.
   - Setup **`kotlin-inject`** for compile-time Dependency Injection.
   - Setup **Jetpack Compose Navigation** with `kotlinx.serialization` for type-safe routing.
   - Configure the testing framework (Kotlin Test, Turbine for Flow verification).

2. **Define Layered Architecture Base** [COMPLETED]
   - Create base classes/interfaces for ViewModels, Repositories, and DataSources.
   - Implement structured concurrency (Coroutines) and state management (`StateFlow`).

3. **Mock Data Layer (Pseudo-API)** [COMPLETED]
   - Define JSON schemas for `Channel`, `Playlist`, `Video`, `Question`, and `UserProgress`.
   - Realistic mock JSON files populated under resources (`channels.json`, `playlists.json`, `videos.json`).
   - Implement `JsonDataSource` to parse bundled JSON files.
   - Create `ScienceRepository` to expose this data and handle progress.

4. **Core Navigation & Main Screens** [COMPLETED]
   - Main Bottom Navigation Bar with 5 tabs:
     - **Home**: Dynamic feed displaying "Pick Up Where You Left Off" card, "Question of the Day", recommended courses, "Most popular courses" (sorted by average views per video), and "Highest rated courses" (sorted by average computed rating).
     - **Discover**: Full-text video search with multiple sort options, category filtering, and channel filter cards.
     - **Progress**: Gamified stats showing watched video count, current streak, and dynamic badge/achievement unlocking.
     - **Store**: DFTBA merch shelf showcasing course-specific merchandise (e.g., Chemistry poster) with direct external hooks.
     - **Settings**: App preferences (profile view, Dark/Light mode toggle, notification preferences, interest toggles).
   - Sub-screens:
     - **Course Details**: Shows course metadata and lists videos within a playlist.
     - **Video Player**: Video layout with title, description, and chapters.
     - **Quiz Screen**: Interactive questionnaire for the selected video.

5. **Testing** [COMPLETED]
   - Unit tests for JSON parsing and `ScienceRepository` logic.
   - Unit tests for ViewModels (`HomeViewModel`, `DiscoverViewModel`, `ProgressViewModel`, `YouViewModel`, `QuizViewModel`) validating state emissions and engagement calculations.

---

## Phase 2: Database & Local Search [IN PROGRESS]
**Goal**: Integrate SQLDelight SQLite database for indexing transcripts and supporting fast local text queries.

1. **Local Database Schema (SQLDelight)** [PENDING]
   - Define `.sq` files for videos, transcripts (FTS5), and user progress.
2. **Data Ingestion Script** [COMPLETED]
   - Python script (`scripts/populate_ratings.py`) updates `videos.json` with likes, views, and comments.
3. **Repository Database Seeding** [PENDING]
   - Seed the SQLite database from the parsed JSON data source on first launch.
4. **Simplified Progress Tracking** [COMPLETED]
   - Last watched video details and completion status saved to DataStore and restored on resume.
5. **Quiz System** [COMPLETED]
   - Interactive quiz UI and validation logic fully built.

---

## Phase 3: Gamification Layer [COMPLETED]
**Goal**: Build a complete gamification experience on top of user progress metrics.

1. **Gamification Engine** [COMPLETED]
   - Multiplatform DataStore-backed state tracking daily streak counts and activity dates.
   - Dynamic badges/achievements: "Early Adopter" (unlocked by default), "Curious Mind" (unlocked by answering quiz/daily question), "Learning Master" (unlocked by watching 10+ videos).

---

## Phase 4: Polish & Pitch Preparation [IN PROGRESS]
**Goal**: Finalize details, UI polish, and prepare a presentation-ready MVP.

1. **Partner-Ready Hooks** [COMPLETED]
   - Merch shelf (Store screen) and external DFTBA link navigation fully implemented.
2. **Visual Polish** [COMPLETED]
   - Custom fonts, HSL curated colors (CrashCourse Teal, Eons Amber, SciShow Blue), smooth transitions, bottom navigation spacing refinements.
3. **Automation Coverage & QA** [COMPLETED]
   - Unit test coverage of all core components. UI manual verification performed via Android emulator.

