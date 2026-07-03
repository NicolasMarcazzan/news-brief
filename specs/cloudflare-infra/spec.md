## ADDED Requirements

### Requirement: Scheduled cron trigger
The Worker SHALL execute the daily briefing pipeline at 07:00 UTC via a cron trigger.

#### Scenario: Cron execution
- **WHEN** the time is 07:00 UTC
- **THEN** the Worker SHALL start the RSS fetch → filter → summarize → store pipeline

### Requirement: KV storage
The daily brief SHALL be stored in Cloudflare Workers KV for retrieval by the Android widget.

#### Scenario: Store brief
- **WHEN** Gemini returns the 6 summaries
- **THEN** the Worker SHALL write the JSON to KV under the key `daily-brief`

#### Scenario: Serve brief
- **WHEN** a GET request hits the Worker URL
- **THEN** the Worker SHALL return the contents of the `daily-brief` KV key as JSON

### Requirement: Secret management
API keys SHALL be stored as Cloudflare Worker secrets, NOT in source code.

#### Scenario: No API keys in source
- **WHEN** inspecting the source repository
- **THEN** no API keys or secrets SHALL appear in any tracked file

### Requirement: .gitignore for sensitive data
The repository SHALL include a `.gitignore` that excludes API keys, secrets, and build artifacts.

#### Scenario: Ignored files
- **WHEN** running `git status`
- **THEN** files containing secrets, `.env`, and build output directories SHALL NOT appear as tracked
