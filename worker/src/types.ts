export interface Article {
  title: string;
  description: string;
  link: string;
  source: string;
  category: string;
}

export interface BriefItem {
  headline: string;
  summary: string;
  source: string;
  category: "global" | "italy" | "markets" | "tech";
}

export interface DailyBrief {
  date: string;
  items: BriefItem[];
}

export interface FeedConfig {
  url: string;
  source: string;
  category: Article["category"];
}

export type LLMResponse = BriefItem[];
