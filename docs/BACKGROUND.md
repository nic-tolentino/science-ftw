I'm thinking of building a Complexly app (crashcourse, scishow, etc), it'll mostly link to the relevant YouTube content, and other related resources like the shop. But I also want to add a great search function, and ideally track which videos have been watched so I can award 'achievements'. Can I access the video transcripts (how?) and identify if a user has viewed a video based on their viewing history? Is there anything else texture wise I could do to make the app useful?

Building a dedicated companion app for Complexly networks (SciShow, CrashCourse, Eons, etc.) sounds like an absolute dream for education nerds and casual learners alike. Bringing deep searchability and a gamified structure to their massive catalogs is an excellent premise.

Let's break down the technical feasibility of your core features, look at the legal/API limitations, and brainstorm how to make the app feel uniquely "Complexly."

1. Accessing Video Transcripts (The Search Function)
To build a genuinely robust search function (e.g., allowing users to search for a highly specific phrase like "Who was the defenestration of Prague actually about?"), you will need the video transcripts.

The Official Route (The Hard Way)
The official YouTube Data API v3 has a Captions endpoint, but it is notoriously restrictive. It typically only allows you to download caption tracks if you are the authenticated owner of the channel. Since you don't own the SciShow or CrashCourse channels, this will throw an authorization error.

The Developer Workaround (The Practical Way)
Most developers bypass the official API restriction using third-party packages that scrape the publicly available closed-caption (CC) tracks that YouTube serves to browsers.

For Python: youtube-transcript-api is the gold standard. It fetches the text and timestamps directly via a video's ID.

For Node.js / JavaScript: youtube-transcript works similarly.

Architecture Tip: Do not fetch transcripts in real-time when a user searches your app; it will be sluggish and YouTube will rate-limit you. Instead, write a backend script to scrape the transcripts for all Complexly videos once, index them using a lightweight search engine like Typesense, Meilisearch, or Algolia, and keep it updated via a cron job that checks for new uploads.

2. Tracking User History & "Achievements"
This is where you hit a major technical roadblock with Google's ecosystem.

The Catch: YouTube Data API Limitations
You cannot access a user's native YouTube watch history. YouTube explicitly deprecated and removed the watchHistory playlist endpoint from their API due to privacy concerns. Even with OAuth 2.0 user authentication, you cannot ask Google, "Did this user watch CrashCourse World History Episode 12?"

The Solution: Local App Tracking
To make achievements work, you must track viewing progress inside your app.

Embed the videos using the YouTube Iframe Player API.

Listen to the player state events (like onStateChange).

When the API fires the event showing the video has ended (or reached 90% completion), trigger a request to your app's backend to check off that video for the user and check if they qualify for an achievement.

3. "Texture" Ideas: Making the App Genuinely Useful
To make it more than just a wrapper for YouTube links, you need features that cater specifically to the Complexly community's habits.

Flashcards & Spaced Repetition (Anki-style)
CrashCourse moves fast. A built-in quiz or flashcard feature for major series would be an incredible utility for students. You could even use an LLM API to ingest the transcripts you pulled and auto-generate basic review questions for each video.

The "Ultimate Complexly Timeline"
Complexly covers history, paleontology (Eons), space (SciShow Space), and climate change (Our Changing Climate). Imagine an interactive, chronological master timeline. Clicking on "The Cretaceous Period" pulls up the relevant Eons videos, CrashCourse History videos, and SciShow biology clips side-by-side.

The "Bizarre Fact of the Day" Widget
Hank and John Green are famous for hyper-fixated, bizarre trivia. A daily home-screen widget featuring a weird fact culled from a transcript, complete with a "Watch the 3-minute explanation" button, would drive great daily active usage.

Podcon / Thought Cafe Integration
Don't just link to DFTBA.com (the shop). Pull in art assets from Thought Cafe (the animation studio behind CrashCourse) if public, or include links to the sister podcasts like Dear Hank & John or The Anthropocene Reviewed when they cover overlapping topics.

How do you plan to handle the database for users? If you are leaning toward mobile (iOS/Android) or a web app, that will dictate the best search indexing stack to use.

Great ideas! Love it! I'm not very keen on an inbuilt player though, because it means users will get ads no (even if they have a subscription - or does Complexly not enable ads on their videos?)? I was instead planning to link out to the YouTube player.



My plan was to use Kotlin Multiplatform for the mobile apps - no website for now. Ideally everything user related would be stored locally for now, to avoid asking the user to create an account (which Complexly would want to control). Once I have an official partnership with Complexly we can look into a backend with accounts and user data.



Is this the way to go, or is there a better approach?

Your strategy of keeping things lean, privacy-focused, and friction-free is incredibly smart—especially when your ultimate goal is an official partnership. Pitching an app to Complexly that already respects user privacy and doesn't try to hijack their audience's data is going to make you look highly professional.

However, moving the video playback entirely out of your app introduces a massive riddle for your core "achievements" feature. Let’s tackle the ad dilemma first, look at the tracking problem, and map out how Kotlin Multiplatform (KMP) fits into this.

1. The Ad Dilemma & The In-App Player
To answer your question directly: Yes, Complexly runs ads on their videos. Educational content relies heavily on AdSense (alongside Patreon and DFTBA merch).

Regarding YouTube Premium users: If a user pays for YouTube Premium, they do not get ads in embedded players, provided they are signed into their Google account on the underlying browser engine running the embed.

That said, linking out to the native YouTube app is a cleaner user experience for premium features (like background play or picture-in-picture). But it breaks your tracking mechanism.

2. The Tracking Riddle: How to Award Achievements Without an In-App Player
If a user clicks a button in your app, bounces out to the native YouTube app, watches a 15-minute CrashCourse video, and returns to your app, your app has absolutely no idea what happened while they were gone. iOS and Android sandboxing prevents you from seeing what other apps are doing.

Since you want to store data locally and avoid accounts, here is how you can gamify this using an "honor system" or clever UI hooks:

Approach A: The "Mark as Watched" Manual Check
Instead of automatic tracking, make checking off a video feel satisfying.

Put a giant, beautiful, tactile "I Finished This Lesson!" button under the video link.

When they tap it, trigger a fun, haptic-feedback animation (confetti, a checkmark, or a satisfying sound effect).

Why it works: It leans into intentionality. For an educational app, users usually want to track their actual progress, so cheating the system by clicking without watching yields no real reward for them anyway.

Approach B: The "Proof of Knowledge" Unlock (Micro-Quizzes)
Instead of tracking time watched, track comprehension.

When they click "Watch Video," your app registers that they opened it.

When they return to your app, unlock a 1-to-3 question multiple-choice quiz based on that video.

Answering correctly officially marks the video as "Completed" and unlocks the achievement.

Why it works: This completely sidesteps the tracking issue, ensures they actually watched it, and perfectly aligns with the educational texture ideas we discussed (like the flashcard system).

3. The Kotlin Multiplatform (KMP) Tech Stack
Kotlin Multiplatform is an excellent choice for this. It allows you to write your business logic, search processing, data models, and local storage once, while building completely native UIs (Compose Multiplatform or swiftUI) so the app feels incredibly premium.

For a local-first, zero-account architecture, here is the ideal KMP stack:

+-------------------------------------------------------------+
|                        UI LAYER                             |
|          Compose Multiplatform  /  Native SwiftUI           |
+-------------------------------------------------------------+
                              |
+-------------------------------------------------------------+
|                    BUSINESS LOGIC LAYER                     |
|         Multiplatform Settings (Encrypted SharedPrefs)      |
|             SQLDelight (Local SQLite Database)              |
+-------------------------------------------------------------+
Local Data Storage
Database: Use SQLDelight. It generates type-safe Kotlin APIs from SQL statements and works flawlessly across iOS and Android. You will use this to store your pre-indexed video catalog, categories, and timestamps.

User Progress: Use Multiplatform Settings (a library that wraps Android SharedPreferences and iOS NSUserDefaults). This is perfect for storing simple key-value pairs like achievements_unlocked_list or user_streak_count.

How to Handle Search Locally
Since you don't want a web backend yet, you have two choices for your search engine:

The Quick Way: Ship a pre-populated SQLite database containing the video metadata and transcripts with the app bundle. Use SQLite's built-in FTS5 (Full-Text Search) extension via SQLDelight to query the transcripts locally on the user's device. It's shockingly fast for datasets under a few hundred megabytes.

The Scalable Way: Keep a tiny, free-tier cloud instance (like Supabase or an Algolia free tier) only to handle search queries, which returns the video IDs to your app. The app still processes everything else locally.

Is this the right way to go?
Absolutely. Building a local-first, high-utility prototype using KMP proves to Complexly that you respect their data boundaries, understand modern mobile architecture, and care about user friction.

By the time you sit down with them, you won't just be pitching an idea; you'll be handing them a fully functioning, beautiful piece of software that seamlessly slots into their ecosystem.

Which tracking approach feels like it fits the vibe of the app better—the satisfying manual check-off, or the quick micro-quiz?

Definitely the quick micro quiz, what's more, it could lead to further quiz questions for people that want to test themselves more thoroughly (achieve higher test grade achievements), and also potentially enable a local LLM chat feature where they can ask questions on the content to better understand it (but that's probably a phase 2 feature, together with user accounts).



Is there anything else I should consider feature wise?

Doubling down on the quiz mechanic is a brilliant move. It transforms the app from a passive video directory into an active learning tool, which aligns perfectly with Complexly’s educational mission. Using local LLMs later for an interactive "tutor" session is a great Phase 2 goal.

To make this app incredibly sticky and clear for a potential partnership pitch, here are a few other specific features and structural ideas tailored to the Complexly universe:

1. Gamifying the Quiz Architecture
Since you are going all-in on quizzes for verification, structure them to feel like an academic journey, but fun.

The "Final Exam" Achievement: Unlocking a standard achievement requires passing the 3-question micro-quiz for an individual video. However, once a user finishes an entire playlist (e.g., CrashCourse Chemistry), unlock a comprehensive 20-question "Final Exam." Passing this grants a master-tier achievement or a digital "Certificate of Completion" they can share.

Streak Safeguards (The "Hank's Day Off" Pass): Educational apps can get stressful if you lose a daily streak. Give users a local "Streak Shield" if they read a specific number of text summaries or do a quick flashcard review instead of a full video quiz on days they are short on time.

2. Smart Video Syncing (Handling "Where was I?")
Because you are linking out to the native YouTube app, users will watch content there and return to your app.

The "Jump Back In" UI Hook: When a user taps a link in your app to watch a video, save the exact timestamp they left at. When they open your app again, display a prominent card on the home screen: "Did you finish World History Episode 14? Take the quiz!" or "Still watching? Jump back in." This creates a seamless bridge between the two apps.

3. Curated "Learning Pathways"
Complexly has thousands of videos, and a massive barrier for new viewers is decision paralysis.

Cross-Channel Playlists: Instead of just listing the videos by channel, create thematic pathways. For example, a "Deep Time" Pathway could curate 5 videos from CrashCourse Big History, 7 from SciShow Space, and 10 from Eons in perfect chronological order.

The "Curriculum" Mode: Allow users to select their current real-world class (e.g., AP US History, High School Biology) and filter the app to only surface videos, flashcards, and quizzes directly relevant to that curriculum.

4. Designing for the Pitch: The "Partner-Ready" Toggle
Since your ultimate goal is an official partnership, build hooks into your Kotlin Multiplatform code right now that show you are planning ahead for them.

The Merch & Patreon Contextual Shelf: Underneath the quiz section for a video, leave a beautifully designed space for "Related Merch." For a CrashCourse Literature video, it could show a placeholder link to the DFTBA Store for the official posters or notebooks.

"Sign In" Placeholder: Create a settings page with a grayed-out or "Coming Soon" section that says "Complexly Account Integration Coming Soon." When you demo the app to their team, this explicitly shows them: "Look, your users' data is local right now, but I built the architecture so we can plug it directly into your single sign-on (SSO) systems later."

Summary of Phase 1 Scope (The MVP Stack)
To keep your build realistic for a solo launch, your KMP app will look like this:

Home Screen: Daily Bizarre Fact widget, current streaks, and "Jump Back In" card.

Browse/Search Screen: Local SQLDelight FTS5 search across all transcripts, filterable by channel or pathway.

Video Detail Screen: Video description, "Launch YouTube" button, and the locked Micro-Quiz.

Profile Screen: Local achievements list, unlocked certificates, and local settings data.

This gives you a powerful, self-contained app requiring zero server maintenance costs while you focus on perfect execution. Have you thought about how you want to author or generate the initial database of quiz questions for the launch?

Can you please give me a detailed summary of what we've discussed - something I can use to start a project plan with an Agentic AI?

Here is a structured, comprehensive project plan layout specifically tailored for an Agentic AI (like an advanced coding agent or software development team). It synthesizes all our technical decisions, feature scope, and architecture constraints into clean, machine-readable specifications.

Project Blueprint: The Complexly Companion App (MVP)
Project Overview & Goals
Target Audience: Education enthusiasts and students tracking content across the Complexly ecosystem (CrashCourse, SciShow, Eons, Study Hall, etc.).

Strategic Goal: Build a fully localized, high-utility mobile prototype to eventually pitch for an official, authenticated partnership with Complexly.

Privacy & Friction Guardrails: Zero mandatory server-side accounts or cloud data collection for Phase 1. All user progress data must reside locally on the device.

1. Core Technical Stack Specification
[UI Layer: Compose Multiplatform (Android) & Native SwiftUI (iOS)]
                                |
[Business Logic: Common Kotlin Multiplatform (KMP) Codebase]
                                |
       +------------------------+------------------------+
       |                                                 |
 [Database: SQLDelight]                   [Preferences: Multiplatform Settings]
 - Video Metadata Inventory               - User Streak Tracking
 - Pre-indexed Transcripts (FTS5)         - Completed Quizzes / Achievements
Mobile Framework
Engine: Kotlin Multiplatform (KMP).

Shared Logic: Common Kotlin for models, network clients (if any), database queries, and quiz evaluation logic.

UI Implementation: Compose Multiplatform for Android / shared views, native SwiftUI for iOS to maximize native premium performance.

Local Storage Stack
Relational Storage: SQLDelight creating type-safe Kotlin wrappers over SQLite.

Key-Value Pairs: Multiplatform Settings wrapping SharedPreferences (Android) and NSUserDefaults (iOS).

2. Technical Solutioning & Features
Feature A: Deep Transcript-Level Local Search
The Constraint: Real-time YouTube API fetching is slow and highly prone to strict rate-limiting.

The Blueprint:

A Python backend script uses youtube-transcript-api to scrape text transcripts and timestamps via video IDs for all historical Complexly episodes.

This data is compiled into a lightweight database file shipped as a compressed asset bundled within the application build.

The KMP app executes full-text searches using SQLite's native FTS5 extension via SQLDelight directly on the user's phone hardware.

Feature B: User Verification via "Proof of Knowledge" (Micro-Quizzes)
The Constraint: Google's sandbox blocks access to the native YouTube app's history, and utilizing an embedded player forces unexpected ads on paying YouTube Premium members.

The Blueprint:

The UI Flow: User selects an item -> App captures the out-intent and logs timestamp -> Launches native YouTube Player via Deep Link -> User watches and steps back into the companion app.

The Verification Hook: Upon return, the app presents a 3-question multiple-choice micro-quiz generated from the episode's transcript. Answering correctly officially transitions the catalog entry to IsCompleted = true and pushes the updated data to local storage.

3. UI/UX Architecture (Phase 1 Scope)
Screen 1: Home Dashboard
Daily Bizarre Fact Widget: Displays a random piece of weird science trivia or history culled directly from the transcript database.

"Jump Back In" UI Hook: A dynamically calculated prompt card showing the last link clicked. (Example: "Did you finish watching World History Episode 14? Prove your knowledge and unlock the quiz!") .

Streak Mechanics: Displays continuous day metrics with a "Streak Shield" (Hank's Day Off Pass). Users can preserve streaks by reading text outlines or handling flashcards if they don't have time to watch a video.

Screen 2: Catalog Discovery & Pathways
Unified Search Engine: Real-time query matching across video titles, descriptions, and structural transcripts.

Learning Pathways: Curated chronological playlists merging cross-channel assets. (Example: A "Deep Time" sequence combining videos across PBS Eons, SciShow Space, and CrashCourse History) .

Curriculum Mode: Toggle filters to map video indexes against real-world educational syllabus markers (e.g., AP U.S. History, High School Biology).

Screen 3: Video Details & Study Arena
Standard descriptive parameters alongside a prominent "Launch on YouTube" action.

Interactive quiz interface running local logic to score and flag completions.

The Contextual Merch Shelf: Pre-coded placeholders mirroring real store shelves (DFTBA Store, Patreon campaigns) dynamically mapped underneath specific topics to show corporate partners monetization potential.

Screen 4: Profile & Achievements Matrix
Gamified reward logs plotting out basic unlocks alongside "Final Exam" certificates awarded when an entire course series playlist is mastered.

Partner-Ready Toggle: A visible, intentionally locked UI block stating: "Complexly Account Integration Coming Soon." (Signals to executives during a demo that the code supports future SSO/central database linking) .

4. Immediate Prompt Sequence for Your AI Agent
Feed these exact prompts to your coding agent one by one to begin execution:

Prompt 1 (Setup): "Initialize a Kotlin Multiplatform mobile project targetting Android (Jetpack Compose) and iOS (SwiftUI). Integrate SQLDelight for local database handling and Multiplatform Settings for persistent data storage. Give me the directory structure."

Prompt 2 (Database Schema): "Write the SQLDelight schema (.sq file) for an offline educational video catalog. We need a table for videos (id, title, channel, pathway, description), a table for transcripts mapped to a virtual FTS5 table for lightning-fast keyword querying, and a user_progress table tracking quiz completion states."

Prompt 3 (Quiz Logic): "Create a Kotlin Multiplatform business logic class that processes user answers for a 3-question micro-quiz. If the answers are valid, update the local SQLite state to 'Completed' via SQLDelight and trigger an incremental reward sequence inside Multiplatform Settings."