package com.creditcardmanager.utils

import android.content.Context
import com.creditcardmanager.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class ExportImportManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    fun exportToJson(banks: List<Bank>, tags: List<Tag>, cards: List<Card>, activities: List<Activity>,
                     reminders: List<Reminder>, transactions: List<Transaction>? = null): String {
        val exportData = ExportData(exportTime = LocalDateTime.now().toString(), banks = banks, tags = tags,
            cards = cards, activities = activities, reminders = reminders, transactions = transactions)
        return gson.toJson(exportData)
    }

    fun importFromJson(json: String): ExportData? {
        return try { gson.fromJson(json, ExportData::class.java) } catch (e: Exception) { null }
    }

    fun saveExportFile(content: String, filename: String = "ccm_backup.json"): File {
        val file = File(context.getExternalFilesDir(null), filename)
        file.writeText(content)
        return file
    }

    fun readImportFile(file: File): String = file.readText()

    private class LocalDateAdapter : com.google.gson.JsonSerializer<LocalDate>, com.google.gson.JsonDeserializer<LocalDate> {
        override fun serialize(src: LocalDate?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?) =
            com.google.gson.JsonPrimitive(src.toString())
        override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?) =
            LocalDate.parse(json?.asString)
    }
    private class LocalDateTimeAdapter : com.google.gson.JsonSerializer<LocalDateTime>, com.google.gson.JsonDeserializer<LocalDateTime> {
        override fun serialize(src: LocalDateTime?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?) =
            com.google.gson.JsonPrimitive(src.toString())
        override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?) =
            LocalDateTime.parse(json?.asString)
    }
}
