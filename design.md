## Context

Personal AI news widget delivering 6 curated summaries daily. Single-user (plus small shared group). Cloudflare Worker backend + Android widget frontend. Zero budget, all free tiers.

## Goals / Non-Goals

**Goals:**
- Fetch from 12-15 diverse RSS feeds daily
- Pre-filter routine crime/tragedy without losing systemic/protest stories
- Groq Llama 3.3 70B selects top 6 by max-entropy coverage across sources
- Return neutral, factual 2-3 sentence summaries
- Display on Android homescreen via Jetpack Glance widget
- Refresh once daily (07:00 UTC)
- Shared brief for a small group via single KV endpoint

**Non-Goals:**
- Per-user personalization or account system
- Push notifications
- Archiving history
- User feedback loop (thumbs up/down)

## Architecture

```
07:00 Cron Trigger (CF Workers)
  │
  ├──► Fetch 12-15 RSS feeds in parallel
  │     (Reuters, AP, BBC, Al Jazeera, Guardian,
  │      ANSA, Il Sole 24 Ore, Corriere,
  │      Yahoo Finance, CNBC,
  │      Ars Technica, MIT Tech Review, TechCrunch, HN,
  │      Nature, ScienceDaily, Phys.org)
  │
  ├──► Stage 1: Keyword pre-filter
  │     Strip routine crime/tragedy unless tagged
  │     as protest/policy/economic/systemic
  │
  ├──► Stage 2: Build prompt with ~50-80 candidate articles
  │     (title + first 150 chars)
  │
  ├──► Groq Llama 3.3 70B → 6 structured summaries
  │     Prompt: max-entropy, neutral, no editorializing
  │
  └──► Store in CF KV → served at /daily-brief.json

Android (07:05 via WorkManager)
  │
  └──► Fetches /daily-brief.json
       └──► Renders Jetpack Glance widget
```

## Decisions

| Decision | Choice | Rationale | Alternatives Considered |
|----------|--------|-----------|------------------------|
| Backend runtime | **Cloudflare Workers (TypeScript)** | Free tier, cron built-in, global edge, KV included, no server management | Python FastAPI on Railway — more familiar but needs server, costs |
| AI model | **Groq Llama 3.3 70B** | Free tier, fast inference, open-weight, 128K context | GPT-4o mini (paid), Claude Haiku (paid), Gemini Flash (free, used previously) |
| News sourcing | **RSS feeds only** | Zero cost, wide coverage, simple parsing | News API (all paid at scale) |
| Pre-filter approach | **Keyword rules + prompt instruction** | Two-stage: cheap deterministic filter first, then AI handles nuance | Pure ML filter (overkill), Pure prompt (more tokens on junk) |
| Widget framework | **Jetpack Glance** | Declarative, Compose-like, modern Android widget standard | RemoteViews (legacy), UAMP (dead) |
| Background fetch | **WorkManager** | Android standard for periodic background work, survives app kill | AlarmManager (less reliable), foreground service (battery drain) |
| Data storage | **CF Workers KV** | Free tier, simple key-value, globally replicated, fits our 1 KB/day payload | R2 (overkill), D1 (relational where not needed) |

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| RSS feed goes down or changes format | Monitor dead feeds via wrangler observability; fallback to skip + log |
| Groq API key exposure in git | `.gitignore` + `wrangler secret put` — API key never in source |
| Groq changes free tier terms | Model choice is abstracted — swap prompt format for another model |
| Widget fetch fails if Worker is cold | KV is always-hot read; Worker cold start < 1s for Node-like runtime |
| Android WorkManager delayed by OS battery optimization | Use `android:minInterval` with `PERIODIC` and accept up to 15min delay — news isn't time-critical |
| Political slant in AI summaries | Prompt engineering + source diversity (Reuters left/right balanced, Al Jazeera for non-Western) |

## Open Questions

- Should the Worker store raw article text or just the Gemini response? (Currently: just response — KV has 1KB limit per value if not using KV's paid tier; actually KV free is 25MB per value, so fine)
- Italian feeds: Il Sole 24 Ore has a paywall. Should we use ANSA + Corriere only? Or add La Repubblica?
