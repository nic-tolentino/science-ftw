# Architecture & Technical Decisions

This document tracks the core architectural decisions made for the Science FTW project.

## 1. UI Framework
**Decision**: Compose Multiplatform (CMP) for both Android and iOS.
*   **Rationale**: Maximizes development velocity for a solo developer/small team while sharing 100% of the UI code. The business logic is strictly decoupled, allowing us to swap in native SwiftUI for iOS in the future if a fully native feel is required for a corporate partnership.
*   **Target Minimums**: Android API 26+, iOS 16+.

## 2. Navigation Framework
**Decision**: Jetpack Compose Navigation for KMP (with `kotlinx.serialization` for Type-Safe Routes).
*   **Rationale**: Navigation crashes are a major source of runtime instability. By using Jetpack Navigation's new type-safe routing API with Kotlin Serialization, routes are defined as data classes/objects. The compiler guarantees that all required arguments are passed correctly, eliminating runtime `IllegalArgumentException` issues associated with string-based route parsing.

## 3. Dependency Injection (DI)
**Decision**: `kotlin-inject`
*   **Rationale**: Service locators like Koin resolve dependencies at runtime, which can lead to app crashes if a module is forgotten. `kotlin-inject` uses Kotlin Symbol Processing (KSP) to generate the dependency graph at compile-time. If a dependency is missing, the app simply will not compile. This fulfills the strict requirement for compile-time safety and high reliability.

## 4. Software Architecture Pattern
**Decision**: `View` -> `ViewModel` -> `UseCase` (optional) -> `Repository` -> `DataSource`
*   **Rationale**: 
    *   **Encapsulation**: UI knows nothing about data origin.
    *   **Testability**: ViewModels and UseCases can be fully unit-tested by mocking the Repository interfaces.
    *   **Flexibility**: The `Repository` layer abstracts how data is loaded. The Repository filters the bundled `JsonDataSource` in-memory, allowing us to swap in a database query layer (e.g. SQLDelight) or a REST API in the future without modifying ViewModels.

## 5. Persistence & Local Storage
**Decision**: JSON (Authoritative Content), DataStore (User State), & Optional SQLDelight.
*   **Rationale**: 
    *   **Content**: Bundled JSON files are the source of truth for the catalog. They map 1:1 to a future REST API and are clean to parse and filter.
    *   **Querying**: Memory-efficient Kotlin collection filtering handles search (title, description, tags, categories) and sorting locally, avoiding the overhead of SQLite database setup for the MVP.
    *   **State**: Multiplatform DataStore/Preferences stores lightweight user state (streaks, preferences, interests, completed videos).
