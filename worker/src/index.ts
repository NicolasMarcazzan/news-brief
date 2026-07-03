import { XMLParser } from "fast-xml-parser";
import { Article, DailyBrief, LLMResponse } from "./types";
import { FEEDS } from "./feeds";
import { buildPrompt } from "./prompt";

const EXCLUDE_PATTERNS = /\b(murder|killed|shooting|stabbing|crash|killed in|bombed|deadly)\b/i;
const INCLUDE_PATTERNS = /\b(sanctions|trade|treaty|summit|protest|policy|economic|market|election|tariff|regulation|reform|climate|pandemic|migration|budget|inflation|crisis|agreement|alliance)\b/i;

interface Env {
  BRIEF_KV: KVNamespace;
  GROQ_API_KEY: string;
  ENVIRONMENT?: string;
}

const xmlParser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: "@_",
});

async function parseFeed(feedUrl: string): Promise<{ title: string; description: string; link: string }[]> {
  const resp = await fetch(feedUrl, {
    headers: { "User-Agent": "NotizieBrief/1.0" },
  });
  if (!resp.ok) return [];

  const xml = await resp.text();
  const parsed = xmlParser.parse(xml);

  const channel = parsed.rss?.channel ?? parsed.feed ?? {};
  const items = channel.item ?? channel.entry ?? [];
  const raw = Array.isArray(items) ? items : [items];

  return raw.map((item: any) => ({
    title: typeof item.title === "object" ? item.title?.["#text"] ?? item.title?._ ?? "" : String(item.title ?? ""),
    description: typeof item.description === "object" ? item.description?.["#text"] ?? item.description?._ ?? "" : String(item.description ?? ""),
    link: typeof item.link === "object" ? item.link?.["@_href"] ?? item.link?.["#text"] ?? "" : String(item.link ?? ""),
  }));
}

function isRelevant(article: { title: string; description: string }): boolean {
  const text = `${article.title} ${article.description}`;
  const excluded = EXCLUDE_PATTERNS.test(text);
  const included = INCLUDE_PATTERNS.test(text);
  return !excluded || included;
}

async function fetchAllFeeds(): Promise<Article[]> {
  const results = await Promise.allSettled(
    FEEDS.map(async (feed) => {
      try {
        const items = await parseFeed(feed.url);
        return items.map((item) => ({
          title: item.title.slice(0, 300),
          description: item.description.replace(/<[^>]*>/g, "").slice(0, 300),
          link: item.link,
          source: feed.source,
          category: feed.category,
        }));
      } catch {
        console.error(`Failed to fetch: ${feed.source}`);
        return [];
      }
    })
  );

  return results
    .flatMap((r) => (r.status === "fulfilled" ? r.value : []))
    .filter((a) => a.title && isRelevant(a))
    .slice(0, 80);
}

async function callGroq(
  articles: Article[],
  apiKey: string
): Promise<LLMResponse> {
  const { system, user } = buildPrompt(articles);

  const resp = await fetch(
    "https://api.groq.com/openai/v1/chat/completions",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: "meta-llama/llama-4-scout-17b-16e-instruct",
        messages: [
          { role: "system", content: system },
          { role: "user", content: user },
        ],
        temperature: 0.3,
        max_tokens: 8192,
      }),
    }
  );

  if (!resp.ok) {
    const err = await resp.text();
    throw new Error(`Groq API error (${resp.status}): ${err}`);
  }

  const data: any = await resp.json();
  const choice = data?.choices?.[0];
  const finishReason = choice?.finish_reason;
  const text = choice?.message?.content;

  if (finishReason && finishReason !== "stop") {
    console.error(`Groq finish reason: ${finishReason}`);
  }

  if (!text) {
    throw new Error(`No response from Groq. finish_reason=${finishReason}`);
  }

  const jsonBlock = text.match(/```(?:json)?\s*([\s\S]*?)```/);
  const jsonText = jsonBlock ? jsonBlock[1].trim() : text.trim();

  const cleaned = jsonText
    .replace(/\n\s*/g, " ")
    .replace(/,\s*([\]}])/g, "$1");

  let parsed: unknown;
  try {
    parsed = JSON.parse(cleaned);
  } catch (err: any) {
    throw new Error(
      `Failed to parse Groq JSON: ${err.message}. Raw text (first 500 chars): ${text.slice(0, 500)}`
    );
  }

  if (!Array.isArray(parsed)) {
    throw new Error(`Groq response is not an array. Got: ${JSON.stringify(parsed).slice(0, 300)}`);
  }
  return parsed as LLMResponse;
}

async function buildBrief(env: Env): Promise<DailyBrief> {
  const articles = await fetchAllFeeds();
  const result = await callGroq(articles, env.GROQ_API_KEY);
  return {
    date: new Date().toISOString().split("T")[0],
    items: result,
  };
}

export default {
  async scheduled(event: ScheduledEvent, env: Env, ctx: ExecutionContext): Promise<void> {
    console.log("Starting daily brief pipeline...");
    const brief = await buildBrief(env);
    await env.BRIEF_KV.put("daily-brief", JSON.stringify(brief));
    console.log("Brief stored in KV");
  },

  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/trigger") {
      try {
        const brief = await buildBrief(env);
        await env.BRIEF_KV.put("daily-brief", JSON.stringify(brief));
        // Same shape as GET / on purpose: the Android client decodes both
        // responses as a bare DailyBrief, so don't wrap this in {ok, brief}.
        return new Response(JSON.stringify(brief), {
          headers: { "Content-Type": "application/json" },
        });
      } catch (err: any) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { "Content-Type": "application/json" },
        });
      }
    }

    const cached = await env.BRIEF_KV.get("daily-brief");
    if (!cached) {
      return new Response(JSON.stringify({ error: "No brief available yet" }), {
        status: 404,
        headers: { "Content-Type": "application/json" },
      });
    }
    return new Response(cached, {
      headers: {
        "Content-Type": "application/json",
        "Cache-Control": "public, max-age=3600",
      },
    });
  },
};
