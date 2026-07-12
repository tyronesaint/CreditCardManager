package com.creditcardmanager.model

import com.google.gson.annotations.SerializedName

data class ExportData(
    @SerializedName("version") val version: String = "1.0",
    @SerializedName("export_time") val exportTime: String,
    @SerializedName("banks") val banks: List<Bank> = emptyList(),
    @SerializedName("tags") val tags: List<Tag> = emptyList(),
    @SerializedName("cards") val cards: List<Card> = emptyList(),
    @SerializedName("activities") val activities: List<Activity> = emptyList(),
    @SerializedName("reminders") val reminders: List<Reminder> = emptyList(),
    @SerializedName("transactions") val transactions: List<Transaction>? = null
)
