# Database Schema (Draft)

This document outlines the proposed local SQLite database schema (originally drafted for Phase 2) for the Science FTW app. Note: Transcript-level indexing has been descoped from the MVP to maintain compliance with YouTube Terms of Service.

## `videos` Table
Stores the core metadata for each educational video.
*   `id`: Text (Primary Key) - The YouTube Video ID.
*   `title`: Text - The title of the video.
*   `channel`: Text - The source channel (e.g., "CrashCourse", "SciShow").
*   `description`: Text - The video description.
*   `published_at`: Integer - Unix timestamp of publication.
*   `duration_seconds`: Integer - Length of the video.
*   `thumbnail_url`: Text - URL to the high-res thumbnail.

## `transcripts` (FTS5 Virtual Table) [DESCOPED]
*Note: To ensure platform compliance, transcript scraping and full-text transcript search have been descoped from the MVP. This section is preserved for legacy architecture reference only.*
*   `video_id`: Text - Foreign key linking to `videos.id`.
*   `text`: Text - The actual spoken text/caption.
*   `start_time`: Real - The timestamp (in seconds) where this text starts.
*   `end_time`: Real - The timestamp (in seconds) where this text ends.

## `user_progress` Table
Tracks the user's interaction with the catalog.
*   `video_id`: Text (Primary Key) - Links to `videos.id`.
*   `status`: Text - Enum-like string: 'UNWATCHED', 'IN_PROGRESS', 'COMPLETED'.
*   `last_watched_timestamp`: Integer - The local time the user last interacted with it.
*   `quiz_score`: Integer - The score achieved on the micro-quiz (if taken).

## `achievements` Table
Stores unlocked rewards.
*   `id`: Text (Primary Key) - Unique identifier for the achievement.
*   `title`: Text - Name of the achievement.
*   `description`: Text - Details of the achievement.
*   `unlocked_at`: Integer - Unix timestamp when the user met the criteria.
*   `icon_asset`: Text - Reference to the local graphic to display.

## `quiz_questions` Table
*(Note: As of the latest architecture updates, questions are primarily authored as embedded arrays inside the `videos.json` JSON structure. If extracted to a relational table in Phase 2, this is the proposed shape.)*
*   `id`: Text (Primary Key) - Unique identifier for the question.
*   `video_id`: Text - Foreign key linking to `videos.id`.
*   `question_text`: Text - The question being asked.
*   `transcript_timestamp_seconds`: Integer - The timestamp in the video where the answer appears.
*   `flags`: Text - JSON string array of flags (e.g., `["INTERESTING", "IMPORTANT"]`).
*   `options`: Text - JSON string array of option objects. Each option object contains `{ "text": String, "isCorrect": Boolean, "explanation": String }`. (Stored as a JSON array for simplicity, as individual options are display-only and never queried individually).
