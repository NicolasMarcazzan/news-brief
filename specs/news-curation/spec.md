## ADDED Requirements

### Requirement: Daily RSS feed ingestion
The system SHALL fetch articles from at least 12 diverse RSS feeds at 07:00 UTC daily.

#### Scenario: Successful parallel fetch
- **WHEN** the cron trigger fires at 07:00 UTC
- **THEN** the Worker fetches all configured RSS feeds in parallel within 30 seconds

#### Scenario: Feed failure tolerance
- **WHEN** one or more RSS feeds are unreachable or return errors
- **THEN** the Worker SHALL skip the failed feeds and continue with the remaining ones, logging the failure

### Requirement: Pre-filtering of routine tragedy
The system SHALL exclude articles about routine local crime, single-victim tragedies, and minor conflicts with no systemic economic/geopolitical impact.

#### Scenario: Exclusion of local crime
- **WHEN** an article describes a murder, shooting, or accident without broader policy/economic context
- **THEN** the article SHALL be excluded from the candidate pool

#### Scenario: Inclusion of protests
- **WHEN** an article describes cross-national protest movements
- **THEN** the article SHALL be included regardless of other exclusion criteria

#### Scenario: Inclusion of systemic conflict
- **WHEN** an article describes a conflict that shifts borders, alliances, trade, or energy markets
- **THEN** the article SHALL be included

### Requirement: Max-entropy selection
The system SHALL select 6 stories using a max-entropy approach: the most broadly covered stories across ideologically diverse sources.

#### Scenario: Selection across sources
- **WHEN** multiple sources report on the same story
- **THEN** that story SHALL be prioritized over stories covered by a single source

#### Scenario: Category distribution
- **WHEN** selecting 6 stories
- **THEN** the output SHALL contain exactly 2 global news, 1 Italian news, 1 financial markets, and 2 AI/tech/science stories

### Requirement: Neutral factual summarization
The system SHALL produce 2-3 sentence summaries with no editorializing, framing, or political slant.

#### Scenario: Neutral output
- **WHEN** Gemini returns a summary
- **THEN** it SHALL contain only factual statements without opinion words (e.g., "tragically", "unfortunately", "landmark")

#### Scenario: Source attribution
- **WHEN** a summary is generated
- **THEN** it SHALL include the source publication name

### Requirement: Groq Llama 3.3 70B integration
The system SHALL use the Google Groq Llama 3.3 70B API for article selection and summarization.

#### Scenario: API call
- **WHEN** the prompt is ready with candidate articles
- **THEN** a single API call to Groq Llama 3.3 70B SHALL return 6 structured summaries
