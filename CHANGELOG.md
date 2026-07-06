# Changelog

All notable changes to the Tertiary Planner Android app.

## [1.0] (versionCode 1) — 2026-07-07

Initial release — a native Android mirror of the iOS Tertiary Planner app.

### Added
- **Assistant chat** — the app's front door: tell the assistant what you need by text or
  voice and it drafts a nicely worded to-do or appointment and saves it instantly, with undo.
- **On-device intelligence** — a deterministic natural-language parser detects dates and
  times ("tomorrow 1pm", "friday 3pm", "12 july"), classifies task vs. appointment, and
  cleans the phrase into a tidy title. Fully offline; nothing leaves the phone.
- **Voice capture** — native Android speech-to-text (`SpeechRecognizer`) with live
  transcript, both in the assistant and a dedicated "Add by Voice" sheet on the Planner tab.
- **Planner list** — active to-dos and appointments grouped by kind; tap any item to edit
  its title, notes, type, or date in the same form used to create it.
- **Built-in calendar** — month grid with a dot on days that have appointments, per-day
  appointment list, and the next five upcoming appointments.
- **Auto-archive** — checking off an item moves it to the Archive automatically; uncheck
  to restore, or clear the archive in one tap.
- **Feedback & About** — house-style tabs (WhatsApp feedback, developer info, version).
- **Persistence** — Room database, on-device only.
- Material 3 / Jetpack Compose UI with the iOS app's indigo brand palette and full
  dark-mode support.

### Play submission
- Submitted for review on 2026-07-07: release **1 (1.0)** on the **Closed testing – Alpha**
  track, testers = "All Testers" email list (19 users), country = Singapore. All 10 App
  content declarations actioned (no ads, no data collection, IARC rating Everyone/3+),
  category Productivity, contact email set. Status: changes in review.
