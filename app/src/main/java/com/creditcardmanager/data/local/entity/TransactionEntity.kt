package com.creditcardmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["cardId"]),
        Index(value = ["tagId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val amount: Double,
    val spendDate: String,
    val tagId: String,
    val channel: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
