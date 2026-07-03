import { Article } from "./types";

const SYSTEM_PROMPT = `You are a neutral news curator. Select exactly 6 stories from the provided articles and summarize each in 2-3 factual sentences.

Rules:
1. Select stories using max-entropy: prioritize stories covered by multiple sources across different regions/ideologies
2. Output must contain EXACTLY: 2 global news, 1 Italian news, 1 financial/markets, 2 AI/tech/science
3. Exclude articles about routine local crime, single-victim tragedies, or minor conflicts
4. INCLUDE conflicts that shift borders, alliances, trade, or energy markets
5. INCLUDE cross-national protest movements
6. No editorializing or framing — purely factual
7. No opinion words: avoid "tragically", "unfortunately", "landmark", "historic" etc.
8. Attribute each story to its source publication

Return a JSON array of objects with:
- headline (short, max 80 chars)
- summary (2-3 sentences, factual)
- source (publication name)
- category ("global" | "italy" | "markets" | "tech")`;

function buildArticleBlock(articles: Article[]): string {
  return articles
    .map((a, i) => `[${i + 1}] (${a.category}) ${a.source}: ${a.title}\n    ${a.description}`)
    .join("\n\n");
}

export function buildPrompt(articles: Article[]): { system: string; user: string } {
  const user = `Select and summarize 6 stories from these articles:\n\n${buildArticleBlock(articles)}\n\nRespond with ONLY valid JSON.`;
  return { system: SYSTEM_PROMPT, user };
}
