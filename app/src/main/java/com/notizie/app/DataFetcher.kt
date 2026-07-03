package com.notizie.app

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Result of a fetch/trigger call. Using a sealed type instead of a nullable
 * DailyBrief? so failures are distinguishable (network vs. worker error vs.
 * bad JSON) instead of all collapsing into "null" and being invisible to the user.
 */
sealed class BriefResult {
    data class Success(val brief: DailyBrief) : BriefResult()
    data class Failure(val message: String) : BriefResult()
}

object DataFetcher {
    // Quick reads from the cache (GET /) should be fast.
    private val readClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // /trigger does real work server-side (RSS fetch -> Gemini call -> KV write),
    // so it needs a much longer timeout or it will fail every time under load.
    private val triggerClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** Strips a trailing slash and any accidental "/trigger" the user may have typed in. */
    fun normalizeBaseUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        while (url.endsWith("/")) url = url.dropLast(1)
        if (url.endsWith("/trigger")) url = url.removeSuffix("/trigger")
        Log.d("DataFetcher", "normalizeBaseUrl: '$rawUrl' -> '$url'")
        return url
    }

    /** Cheap read of whatever is currently cached in KV. Used for daily background refresh. */
    fun fetchBrief(baseUrl: String): BriefResult = execute(readClient, baseUrl)

    /** Forces the worker to run the full pipeline (RSS -> Gemini -> KV) and returns the fresh result. */
    fun triggerBrief(baseUrl: String): BriefResult = execute(triggerClient, "$baseUrl/trigger")

    private fun execute(client: OkHttpClient, url: String): BriefResult {
        Log.d("DataFetcher", "execute: fetching $url")
        val startMs = System.currentTimeMillis()
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startMs
                val body = response.body?.string()
                    ?: return BriefResult.Failure("Empty response from server").also {
                        Log.e("DataFetcher", "execute: empty body after ${elapsed}ms, code=${response.code}")
                    }

                Log.d("DataFetcher", "execute: ${response.code} after ${elapsed}ms, body=${body.take(200)}")

                if (!response.isSuccessful) {
                    return BriefResult.Failure("Server error (${response.code}): ${body.take(200)}").also {
                        Log.e("DataFetcher", "execute: HTTP error ${response.code}")
                    }
                }

                try {
                    val parsed = json.decodeFromString<DailyBrief>(body)
                    Log.d("DataFetcher", "execute: parsed OK, date=${parsed.date}, items=${parsed.items.size}")
                    BriefResult.Success(parsed)
                } catch (e: Exception) {
                    Log.e("DataFetcher", "execute: JSON parse error: ${e.message}")
                    BriefResult.Failure("Could not parse response: ${e.message}")
                }
            }
        } catch (e: IOException) {
            Log.e("DataFetcher", "execute: IO error after ${System.currentTimeMillis() - startMs}ms: ${e.message}")
            BriefResult.Failure("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e("DataFetcher", "execute: unexpected error: ${e.message}")
            BriefResult.Failure("Unexpected error: ${e.message}")
        }
    }
}
