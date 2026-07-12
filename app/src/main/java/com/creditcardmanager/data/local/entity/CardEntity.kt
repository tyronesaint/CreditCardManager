package com.creditcardmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.creditcardmanager.model.enums.CardStatus
import com.creditcardmanager.model.enums.DueDayType

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = BankEntity::class,
            parentColumns = ["id"],
            childColumns = ["bankId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CardEntity(
    @PrimaryKey val id: String,
    val bankId: String,
    val name: String,
    val last4: String? = null,
    val creditLimit: Double? = null,
    val status: CardStatus = CardStatus.ACTIVE,
    val statementDay: Int,
    val dueDayType: DueDayType,
    val dueDay: Int? = null,
    val dueIntervalDays: Int? = null,
    val annualFeeEnabled: Boolean = false,
    val annualFeeStartDate: String? = null,
    val annualFeeRuleJson: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
