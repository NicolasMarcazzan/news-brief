## ADDED Requirements

### Requirement: Homescreen widget display
The Android app SHALL provide a homescreen widget that displays all 6 story summaries.

#### Scenario: Widget rendering
- **WHEN** the widget loads
- **THEN** it SHALL show 6 cards, each with headline and 2-3 sentence summary

#### Scenario: Scrollable content
- **WHEN** the widget content exceeds available screen space
- **THEN** the widget SHALL scroll vertically

### Requirement: Daily background refresh
The widget SHALL update automatically once per day by fetching the latest brief from the Cloudflare Worker.

#### Scenario: Scheduled refresh
- **WHEN** the WorkManager triggers the daily job
- **THEN** the app SHALL fetch /daily-brief.json from the Worker URL and update the widget

#### Scenario: Refresh failure handling
- **WHEN** the fetch fails (network error, Worker down)
- **THEN** the widget SHALL continue showing the last successful brief

### Requirement: Jetpack Glance implementation
The widget SHALL be built with Jetpack Glance.

#### Scenario: Widget provider
- **WHEN** the app is installed
- **THEN** the widget SHALL appear in the user's widget picker under the app name

### Requirement: Configurable Worker URL
The app SHALL support configuring the Cloudflare Worker URL.

#### Scenario: URL configuration
- **WHEN** the app first runs
- **THEN** it SHALL prompt for the Worker URL (or hardcode for the single-user case)
- **AND** store it in SharedPreferences
