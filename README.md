# NEWS BRIEF

A personal daily news brief. A Cloudflare Worker reads ~15 RSS feeds every morning, an AI picks the 6 stories that actually matter and writes short, neutral summaries, and an Android app shows them to you in a clean feed — no doom-scrolling, no algorithm, no ads.

## Why this is fire

- **Fast to read.** 6 stories, 2–3 sentences each. Under 5 minutes, once a day.
- **Neutral by design.** The AI is prompted to stay factual and skip routine crime/tragedy clickbait, keeping only stories with real global/systemic weight (policy, trade, elections, climate, conflict that shifts borders or alliances, etc).
- **Diverse sources.** Reuters, BBC, NYT, Al Jazeera, The Guardian, ANSA, Corriere, Yahoo Finance, CNBC, Ars Technica, MIT Tech Review, Hacker News, Nature, ScienceDaily, Phys.org — global, Italian, markets, and tech/science all in one brief.
- **Free to run.** Cloudflare Workers + KV free tier for the backend, Groq's free tier for the AI. No servers to maintain, no API bills.
- **Yours.** Runs on your own Cloudflare account with your own API key — nobody else sees your data, and you can point it at whatever feeds you want.

## How it works

```
Every day at 07:00 UTC (Cloudflare cron)
  → Worker fetches ~15 RSS feeds
  → Filters out routine crime/tragedy (keeps policy/protest/economic stories)
  → Groq's Llama model picks the 6 most broadly-covered stories and
    writes a neutral 2–3 sentence summary for each
  → Result is cached in Cloudflare KV

Android app
  → Reads the cached brief and shows it as a scrollable feed
  → A "Fetch today's news" button can force an on-demand refresh
  → A background job re-syncs the cache once a day automatically
```

## Project layout

```
notizie-brief/
├── worker/     Cloudflare Worker (TypeScript) — fetches, filters, summarizes, caches
└── app/        Android app (Kotlin) — displays the daily brief
```

## Quick setup

You need two things: a Cloudflare account (free) and a Groq API key (free).

### 1. Backend (Cloudflare Worker)

```bash
cd notizie-brief/worker
npm install

# create a KV namespace to store the daily brief
npx wrangler kv namespace create BRIEF_KV
```

Copy the `id` it prints into `wrangler.toml` (copy `wrangler.toml.example` if you don't have one yet), then add your Groq key as a secret:

```bash
cp wrangler.toml.example wrangler.toml   # then paste your KV id into it
npx wrangler secret put GROQ_API_KEY     # paste your key from console.groq.com when prompted
npx wrangler deploy
```

Wrangler prints your Worker's URL when it's done, something like:
```
https://notizie-brief.<your-subdomain>.workers.dev
```
Keep that URL — you'll paste it into the Android app next. It refreshes itself daily via cron, but you can also test it right away by opening `<your-worker-url>/trigger` in a browser.

### 2. App (Android)

```bash
cd notizie-brief
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

(If `./gradlew` complains it can't find the SDK, open the project once in Android Studio — it sets up `local.properties` for you — then go back to the command line.)

Open the app, tap the ⚙ icon, paste your Worker URL from step 1, hit **Save & Start**, then **Fetch today's news**. From then on it refreshes automatically once a day.

## Customizing

- **Change the sources:** edit `worker/src/feeds.ts`
- **Change what gets filtered out / kept:** edit `EXCLUDE_PATTERNS` / `INCLUDE_PATTERNS` in `worker/src/index.ts`
- **Change the summarization style:** edit `worker/src/prompt.ts`
- **Change the refresh time:** edit the cron schedule in `wrangler.toml`


# Made by Nick ;)
