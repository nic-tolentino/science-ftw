# Complexly Academy

## Overview
Complexly Academy is a privacy-first, locally-powered mobile application designed for fans of educational YouTube content (e.g., CrashCourse, SciShow, Eons). 

Rather than building a basic video wrapper, this app acts as a highly curated study companion. It enhances the learning experience by offering offline search, gamified progress tracking, curated learning pathways, and hand-curated quizzes—all starting with locally bundled data and zero mandatory server-side accounts.

## Key Features
* **Curated Content & Pathways**: Explore videos by courses, themes, or chronologically.
* **Proof of Knowledge (Quizzes)**: Take micro-quizzes based on lesson concepts to earn achievements.
* **Gamified Tracking**: Earn achievements, manage daily streaks, and view progress on learning pathways.
* **Deep Local Search**: Search across courses, video titles, descriptions, categories, and tags.
* **Merch Shelf & Patreon Hooks**: Showcase DFTBA merch shelves and direct donation pathways.
* **Partner-Ready Architecture**: Built with future SSO and backend integrations in mind.

## Tech Stack
* **Architecture**: Kotlin Multiplatform (KMP)
* **UI Layer**: Compose Multiplatform (Android & iOS)
* **Architecture Pattern**: View - ViewModel - UseCase / Repository - DataSource
* **Dependency Injection**: `kotlin-inject` (compile-time safety)
* **Navigation**: Jetpack Compose Navigation with Type-Safe Routes (`kotlinx.serialization`)

## Documentation & ADRs
We maintain a rigorous documentation culture. All major architectural decisions and feature blueprints are documented in the `/docs` directory.
- [Project Plan](docs/PLAN.md)
- [Architecture Decisions](docs/ARCHITECTURE_DECISIONS.md)
- [UI Architecture](docs/UI_ARCHITECTURE.md)
- [Database Schema (Draft)](docs/DATABASE_SCHEMA.md)

## Automation Test Coverage
All features are to be covered by automated tests. Important flows and features are to be covered by extensive unit tests to ensure correctness and prevent regressions.

## Legal / Compliance Note
*To ensure complete platform compliance, transcript scraping has been descoped from the MVP. The application is powered entirely by static, hand-curated metadata and quiz datasets, avoiding any dependency on scraped content or YouTube Terms of Service grey areas. Future expansions (such as transcript-level search or automated question generation) will be introduced only through official creator partnerships and official API integrations.*
