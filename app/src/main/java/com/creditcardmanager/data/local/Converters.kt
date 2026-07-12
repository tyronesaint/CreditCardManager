package com.creditcardmanager.data.local

import androidx.room.TypeConverter
import com.creditcardmanager.model.enums.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    @TypeConverter fun fromCardStatus(value: CardStatus): String = value.name
    @TypeConverter fun toCardStatus(value: String): CardStatus = CardStatus.valueOf(value)
    @TypeConverter fun fromDueDayType(value: DueDayType): String = value.name
    @TypeConverter fun toDueDayType(value: String): DueDayType = DueDayType.valueOf(value)
    @TypeConverter fun fromActivityLevel(value: ActivityLevel): String = value.name
    @TypeConverter fun toActivityLevel(value: String): ActivityLevel = ActivityLevel.valueOf(value)
    @TypeConverter fun fromActivityType(value: ActivityType): String = value.name
    @TypeConverter fun toActivityType(value: String): ActivityType = ActivityType.valueOf(value)
    @TypeConverter fun fromPeriodType(value: PeriodType): String = value.name
    @TypeConverter fun toPeriodType(value: String): PeriodType = PeriodType.valueOf(value)
    @TypeConverter fun fromSourceType(value: SourceType): String = value.name
    @TypeConverter fun toSourceType(value: String): SourceType = SourceType.valueOf(value)
    @TypeConverter fun fromStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
    @TypeConverter fun toStringList(list: List<String>): String = gson.toJson(list)
}
