# Quiz Authoring Strategy

A critical component of the Science FTW app is the "Proof of Knowledge" system, where users must pass a 3-question micro-quiz to mark a video as completed and earn achievements. This document outlines how these quizzes are generated and managed.

## The Challenge
Complexly channels have thousands of videos. Manually writing 3 questions for every video is unscalable for a solo developer or small team prior to launch.

## The Strategy: Automated Pipeline with LLMs

### 1. The Generation Pipeline (Developer-Side Automation)
To solve the content authoring bottleneck for the MVP, we use a **local, developer-only build-time pipeline**. The client-side mobile application contains *zero* scraping code and makes *zero* runtime scraping calls.
*   **Transcript Ingestion (Local Only)**: A developer-run Python script downloads video transcripts locally (e.g. using `youtube-transcript-api` or downloading official SRT files).
*   **LLM Processing**: The script passes the full transcript text to a Large Language Model (e.g., Google Gemini Pro API) running on the developer's local machine.
*   **Prompt**: The LLM is explicitly prompted to act as an educator, extract 3 key concepts/facts from the transcript, and write one high-quality multiple-choice question for each.
*   **Output Constraint**: The LLM returns structured JSON matching our embedded question structure. Each question includes `questionText`, `transcriptTimestampSeconds` (linking back to the lesson moment), and an `options` array.
*   **Per-Option Explanations**: The generator creates explanations for both the correct and incorrect options, reinforcing the learning material pedagogically.

### 2. Quality Control & Curation
*   The output of the developer script is written directly to the bundled resources file (`videos.json`).
*   This file is reviewed and curated by the developer prior to compiling the final build.
*   This curation ensures high educational standards and lets the developer manually override questions without touching app code.

### 3. Bundled Delivery (Client Compliance)
*   The generated quizzes are packaged as a static JSON resource inside the app bundle.
*   **The client app remains completely compliant**: It does not scrap transcripts at runtime or feature transcript searches, avoiding YouTube Terms of Service issues. It runs 100% offline with zero recurring LLM API costs.

## Future Roadmap (Phase 3+)
If the app secures an official partnership:
1.  **Community Sourcing**: Introduce a feature where users can suggest better questions, which are reviewed and pushed in over-the-air database updates.
2.  **Live Socratic Tutor**: Re-introduce a live LLM connection where users can chat with an AI "tutor" about the video content for deeper learning.
