## Setup for new users (cloning this repo)

```bash
# 1. Install dependencies
sudo pacman -S jdk21-openjdk gradle android-tools nodejs
yay -S android-sdk-cmdline-tools-latest

# 2. Android SDK
export ANDROID_HOME=/opt/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
sdkmanager --licenses && sdkmanager "platforms;android-34" "build-tools;34.0.0"

# 3. Configure Worker
cp worker/wrangler.toml.example worker/wrangler.toml
# Run: npx wrangler kv namespace create "BRIEF_KV"
# Copy the returned ID into wrangler.toml

# 4. Gradle wrapper
gradle wrapper
```

## Prerequisites

- [x] Cloudflare account (free)
- [x] Google AI Studio API key (free)
- [ ] Android CLI tools installed — run commands above
- [ ] Java 21+ installed
- [ ] Node.js 18+ installed

## Implementation Tasks

### CF Worker ✓

- [x] Create `worker/` with `wrangler.toml`, `package.json`, `tsconfig.json`
- [x] Write `src/feeds.ts` — 15 RSS feeds across all categories
- [x] Write `src/prompt.ts` — system + user prompt for Gemini
- [x] Write `src/types.ts` — TypeScript interfaces
- [x] Write `src/index.ts` — main Worker (RSS fetch → filter → Gemini → KV store)
- [x] `/trigger` endpoint — manual pipeline execution
- [x] **Deployed**: `npx wrangler deploy` — live at `notizie-brief.supernotizieoggi.workers.dev`

### Android Widget ✓

- [x] Create `app/build.gradle.kts` with Glance + WorkManager deps
- [x] Create `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- [x] Write `NotizieWidget.kt` — Glance widget (6 cards + ↻ Refresh button)
- [x] Write `RefreshWorker.kt` — WorkManager daily refresh
- [x] Write `RefreshAction.kt` — Glance action callback for refresh button
- [x] Write `DataFetcher.kt` — OkHttp client
- [x] Write `MainActivity.kt` — URL config screen (plain Views, no Compose)
- [x] Write `AndroidManifest.xml` with widget receiver
- [x] **Built**: `./gradlew assembleDebug` — APK at `app/build/outputs/apk/debug/app-debug.apk`
- [ ] **Sideload**: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Security & Config ✓

- [x] Create `.gitignore` — excludes .env, wrangler.toml, build/, node_modules/
- [x] Create `worker/.env.example` — placeholder for API key
- [x] Create `worker/wrangler.toml.example` — placeholder for KV namespace
- [x] No secrets in tracked files

## Verification

- [x] Worker deployed and reachable
- [ ] `/trigger` works — run `curl https://notizie-brief.supernotizieoggi.workers.dev/trigger`
- [ ] Install APK on phone — widget renders correctly
- [ ] Tap ↻ Refresh in widget — triggers update
- [ ] Wait for next 07:00 cron — auto-refresh works
- [ ] Test with phone in airplane mode — widget shows cached brief
