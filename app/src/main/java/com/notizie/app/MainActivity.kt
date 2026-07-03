package com.notizie.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.abs
import java.time.Duration
import java.time.Instant

/**
 * Shows the daily brief directly in the app. There used to be a home-screen
 * widget for this (Glance), but Glance widgets aren't supported in this build
 * environment, so the same news is now rendered as a scrollable feed here
 * instead. All the actual data plumbing (DataFetcher, BriefData, RefreshWorker,
 * cached_brief in SharedPreferences) is unchanged.
 */
class MainActivity : AppCompatActivity() {

    private val bg = Color.parseColor("#0F1115")
    private val surface = Color.parseColor("#1A1D24")
    private val accent = Color.parseColor("#7C5CFF")
    private val textPrimary = Color.parseColor("#F5F5F7")
    private val textSecondary = Color.parseColor("#9A9CA5")

    private val categoryPalette = listOf(
        "#7C5CFF", "#00C2A8", "#FF8A3D", "#3D9BFF", "#E14FD1", "#4CD97B", "#FF5D6C"
    )

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var newsContainer: LinearLayout
    private lateinit var subtitleText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var settingsPanel: LinearLayout
    private lateinit var urlInput: EditText
    private lateinit var refreshBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("notizie", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("worker_url", "")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        // --- Header ---
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(24), dp(8), dp(12))
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = "Notizie"
            setTextColor(textPrimary)
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        subtitleText = TextView(this).apply {
            text = "Your daily brief"
            setTextColor(textSecondary)
            textSize = 13f
            setPadding(0, dp(2), 0, 0)
        }
        titleCol.addView(subtitleText)
        header.addView(titleCol)

        val settingsBtn = TextView(this).apply {
            text = "⚙"
            textSize = 22f
            setTextColor(textSecondary)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                settingsPanel.visibility =
                    if (settingsPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        header.addView(settingsBtn)
        root.addView(header)

        // --- Settings panel (worker URL) ---
        settingsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), dp(16))
            visibility = if (savedUrl.isNullOrBlank()) View.VISIBLE else View.GONE
        }
        settingsPanel.addView(TextView(this).apply {
            text = "Worker base URL (no path, e.g. without /trigger)"
            setTextColor(textSecondary)
            textSize = 12f
            setPadding(0, 0, 0, dp(6))
        })
        urlInput = EditText(this).apply {
            setText(savedUrl)
            hint = "https://notizie-brief.supernotizieoggi.workers.dev"
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            background = roundedDrawable(surface, dp(10))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        settingsPanel.addView(urlInput)
        settingsPanel.addView(Button(this).apply {
            text = "Save & Start"
            setTextColor(Color.WHITE)
            background = roundedDrawable(accent, dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            setOnClickListener {
                val url = DataFetcher.normalizeBaseUrl(urlInput.text.toString())
                if (url.isNotEmpty()) {
                    Log.d("MainActivity", "save: url='$url'")
                    prefs.edit().putString("worker_url", url).apply()
                    urlInput.setText(url)
                    RefreshWorker.schedule(this@MainActivity)
                    refreshBtn.isEnabled = true
                    settingsPanel.visibility = View.GONE
                    Toast.makeText(
                        this@MainActivity,
                        "Saved! Daily auto-refresh scheduled. Tap \"Fetch today's news\" to get today's brief immediately.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this@MainActivity, "Enter a URL first.", Toast.LENGTH_SHORT).show()
                }
            }
        })
        root.addView(settingsPanel)

        // --- Refresh button ---
        val refreshRow = LinearLayout(this).apply {
            setPadding(dp(20), 0, dp(20), dp(10))
        }
        refreshBtn = Button(this).apply {
            text = "⟳   Fetch today's news"
            setTextColor(Color.WHITE)
            background = roundedDrawable(accent, dp(10))
            isEnabled = !savedUrl.isNullOrBlank()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        refreshRow.addView(refreshBtn)
        root.addView(refreshRow)

        progressBar = ProgressBar(this).apply { visibility = View.GONE }
        root.addView(progressBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(4)
            bottomMargin = dp(4)
        })

        // --- News feed ---
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }
        newsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(24))
        }
        scroll.addView(newsContainer)
        root.addView(scroll)

        setContentView(root)
        showEmptyState("No news yet. Configure your worker URL above, then tap \"Fetch today's news\".")

        // Show whatever is cached immediately, so the feed works offline / on cold start.
        prefs.getString("cached_brief", null)?.let { cached ->
            try {
                renderBrief(Json.decodeFromString(DailyBrief.serializer(), cached))
            } catch (e: Exception) {
                Log.e("MainActivity", "cached brief parse failed: ${e.message}")
            }
        }

        refreshBtn.setOnClickListener {
            val url = DataFetcher.normalizeBaseUrl(urlInput.text.toString())
            if (url.isEmpty()) {
                Toast.makeText(this, "Enter and save a URL first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            refreshBtn.isEnabled = false
            progressBar.visibility = View.VISIBLE
            subtitleText.text = "Fetching today's brief (this calls Gemini, can take a few seconds)…"

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    DataFetcher.triggerBrief(url)
                }
                refreshBtn.isEnabled = true
                progressBar.visibility = View.GONE
                when (result) {
                    is BriefResult.Success -> {
                        Log.d("MainActivity", "trigger OK: date=${result.brief.date}, items=${result.brief.items.size}")
                        prefs.edit()
                            .putString(
                                "cached_brief",
                                Json.encodeToString(DailyBrief.serializer(), result.brief)
                            )
                            .putLong("last_fetch_time", System.currentTimeMillis())
                            .commit()
                        renderBrief(result.brief)
                    }
                    is BriefResult.Failure -> {
                        Log.e("MainActivity", "trigger failed: ${result.message}")
                        subtitleText.text = "Failed: ${result.message}"
                        Toast.makeText(this@MainActivity, "Failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun renderBrief(brief: DailyBrief) {
        subtitleText.text = "${brief.items.size} stories · ${brief.date}"
        newsContainer.removeAllViews()
        if (brief.items.isEmpty()) {
            showEmptyState("No stories in today's brief.")
            return
        }
        val lastFetchTime = prefs.getLong("last_fetch_time", 0L)
        if (lastFetchTime > 0L) {
            newsContainer.addView(TextView(this).apply {
                text = "Last fetch: ${relativeTime(lastFetchTime)}"
                setTextColor(textSecondary)
                textSize = 12f
                setPadding(0, 0, 0, dp(12))
            })
        }
        brief.items.forEach { item -> newsContainer.addView(buildNewsCard(item)) }
    }

    private fun showEmptyState(message: String) {
        newsContainer.removeAllViews()
        newsContainer.addView(TextView(this).apply {
            text = message
            setTextColor(textSecondary)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(60), dp(12), dp(20))
        })
    }

    private fun buildNewsCard(item: BriefItem): View {
        val accentForCategory = Color.parseColor(
            categoryPalette[abs(item.category.hashCode()) % categoryPalette.size]
        )

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(surface, dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        card.addView(TextView(this).apply {
            text = item.category.uppercase()
            setTextColor(accentForCategory)
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = 0.08f
            setPadding(0, 0, 0, dp(6))
        })

        card.addView(TextView(this).apply {
            text = item.headline
            setTextColor(textPrimary)
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        })

        card.addView(TextView(this).apply {
            text = item.summary
            setTextColor(textSecondary)
            textSize = 14f
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(0, 0, 0, dp(10))
        })

        card.addView(TextView(this).apply {
            text = item.source
            setTextColor(accentForCategory)
            textSize = 12f
            setTypeface(typeface, Typeface.ITALIC)
        })

        return card
    }

    private fun roundedDrawable(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun relativeTime(timestampMillis: Long): String {
        val now = Instant.now()
        val then = Instant.ofEpochMilli(timestampMillis)
        val d = Duration.between(then, now)

        return when {
            d.isNegative -> "just now"
            d.seconds < 60 -> "just now"
            d.seconds < 3600 -> "${d.toMinutes()} min ago"
            d.toHours() < 24 -> "${d.toHours()} h ago"
            d.toDays() < 7 -> "${d.toDays()} days ago"
            d.toDays() < 30 -> "${d.toDays() / 7} weeks ago"
            d.toDays() < 365 -> "${d.toDays() / 30} months ago"
            else -> "${d.toDays() / 365} years ago"
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}
