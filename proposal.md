## Why

Daily news consumption is overwhelming — engagement-driven algorithms push outrage, tragedy, and clickbait. Existing apps drown out stories that actually matter to a single individual's life. This project builds a personal AI-driven Android widget that delivers 6 concise, neutral summaries every morning in under 5 minutes of reading.

## What Changes

- **Cloudflare Worker** fetching 12-15 RSS feeds daily at 07:00, pre-filtering routine tragedy/crime, and using **Groq Llama 3.3 70B** to select and summarize the 6 most broadly-covered stories (max-entropy selection across ideologically diverse sources)
- **Android widget** (Jetpack Glance) displaying 2-3 sentence summaries on the home screen, refreshed daily via WorkManager
- **Shared KV store** on Cloudflare Workers KV — multiple users (small group) share the same daily brief
- **No political slant**: AI prompted for purely factual, neutral summaries. Conflict stories included only if they shift borders, alliances, trade, energy, or involve cross-national protests
- **No news API costs**: RSS-only ingestion, Google Gemini Free API for curation

## Capabilities

### New Capabilities
- `news-curation`: Daily RSS ingestion, pre-filtering, Gemini-powered max-entropy selection, neutral summarization into 6 stories (2 global, 1 IT, 1 markets, 2 AI/tech/science)
- `android-widget`: Android homescreen widget (Jetpack Glance), shows full summaries, daily refresh via WorkManager
- `cloudflare-infra`: Cloudflare Worker with KV storage, cron trigger (07:00 daily), wrangler config, secret management

### Modified Capabilities

None

## Impact

- **New dependency**: Cloudflare Workers (free tier) + KV (free tier)
- **New dependency**: Groq API (free tier, API key required)
- **New code**: TypeScript Worker (~200 lines) and Kotlin Android app (~300 lines)
- **Configuration**: wrangler.toml with secrets, build.gradle.kts with Glance dependencies
- **No changes** to existing systems
