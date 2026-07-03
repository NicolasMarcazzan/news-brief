package com.notizie.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Daily background refresh. This intentionally reads the cache (GET /) rather
 * than calling /trigger: the Cloudflare cron (07:00 UTC) is what actually runs
 * the RSS -> Gemini pipeline once a day. This worker just syncs the cached
 * result down to the device so the app has today's brief ready without the
 * user needing to open it and manually fetch. Calling /trigger here would
 * burn a Gemini API call every 24h per device for no benefit, and risks
 * hitting the free-tier rate limit.
 */
class RefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("notizie", Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("worker_url", null) ?: run {
            Log.e("RefreshWorker", "no worker_url configured")
            return Result.failure()
        }

        Log.d("RefreshWorker", "doWork: baseUrl=$baseUrl")
        return when (val result = DataFetcher.fetchBrief(baseUrl)) {
            is BriefResult.Success -> {
                Log.d("RefreshWorker", "OK: date=${result.brief.date}, items=${result.brief.items.size}")
                prefs.edit()
                    .putString("cached_brief", Json.encodeToString(result.brief))
                    .putLong("last_fetch_time", System.currentTimeMillis())
                    .commit()
                Result.success()
            }
            is BriefResult.Failure -> {
                Log.e("RefreshWorker", "fetch failed: ${result.message}")
                Result.retry()
            }
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_brief_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
