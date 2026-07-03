package com.notizie.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyBrief(
    val date: String,
    val items: List<BriefItem>
)

@Serializable
data class BriefItem(
    val headline: String,
    val summary: String,
    val source: String,
    val category: String
)
