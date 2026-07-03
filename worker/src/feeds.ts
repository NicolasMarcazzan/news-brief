import { FeedConfig } from "./types";

export const FEEDS: FeedConfig[] = [
  // Global
  { url: "https://feeds.reuters.com/reuters/topNews", source: "Reuters", category: "global" },
  { url: "https://rss.nytimes.com/services/xml/rss/nyt/World.xml", source: "NYT", category: "global" },
  { url: "http://feeds.bbci.co.uk/news/rss.xml", source: "BBC", category: "global" },
  { url: "https://www.aljazeera.com/xml/rss/all.xml", source: "Al Jazeera", category: "global" },
  { url: "https://www.theguardian.com/world/rss", source: "The Guardian", category: "global" },

  // Italy
  { url: "https://www.ansa.it/sito/ansait_rss.xml", source: "ANSA", category: "italy" },
  { url: "https://www.corriere.it/rss/homepage.xml", source: "Corriere della Sera", category: "italy" },

  // Markets
  { url: "https://finance.yahoo.com/news/rssindex", source: "Yahoo Finance", category: "markets" },
  { url: "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=100003114", source: "CNBC", category: "markets" },

  // AI / Tech
  { url: "https://feeds.arstechnica.com/arstechnica/index", source: "Ars Technica", category: "tech" },
  { url: "https://www.technologyreview.com/feed/", source: "MIT Tech Review", category: "tech" },
  { url: "https://news.ycombinator.com/rss", source: "Hacker News", category: "tech" },

  // Science
  { url: "https://www.nature.com/nature.rss", source: "Nature", category: "tech" },
  { url: "https://www.sciencedaily.com/rss/all.xml", source: "ScienceDaily", category: "tech" },
  { url: "https://phys.org/rss-feed/", source: "Phys.org", category: "tech" },
];
