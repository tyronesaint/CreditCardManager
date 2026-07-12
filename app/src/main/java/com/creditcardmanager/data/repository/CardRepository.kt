package com.creditcardmanager.data.repository

import com.creditcardmanager.data.local.dao.CardDao
import com.creditcardmanager.data.local.entity.CardEntity
import com.creditcardmanager.model.AnnualFeeRule
import com.creditcardmanager.model.Card
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(private val cardDao: CardDao, private val gson: Gson) {
    fun getAllCards(): Flow<List<Card>> = cardDao.getAll().map { list -> list.map { it.toModel() } }
    fun getActiveCards(): Flow<List<Card>> = cardDao.getAllActive().map { list -> list.map { it.toModel() } }
    fun getCardsByBankId(bankId: String): Flow<List<Card>> = cardDao.getByBankId(bankId).map { list -> list.map { it.toModel() } }
    suspend fun getCardById(id: String): Card? = cardDao.getById(id)?.toModel()
    suspend fun saveCard(card: Card) = cardDao.insert(card.toEntity())
    suspend fun saveCards(cards: List<Card>) = cardDao.insertAll(cards.map { it.toEntity() })
    suspend fun updateCard(card: Card) = cardDao.update(card.toEntity())
    suspend fun deleteCard(card: Card) = cardDao.delete(card.toEntity())
    suspend fun deleteAll() = cardDao.deleteAll()
    suspend fun getActiveCount(): Int = cardDao.getActiveCount()
    private fun CardEntity.toModel(): Card {
        return Card(id = id, bankId = bankId, name = name, last4 = last4, creditLimit = creditLimit, status = status,
            statementDay = statementDay, dueDayType = dueDayType, dueDay = dueDay, dueIntervalDays = dueIntervalDays,
            annualFeeEnabled = annualFeeEnabled, annualFeeStartDate = annualFeeStartDate?.let { LocalDate.parse(it) },
            annualFeeRule = annualFeeRuleJson?.let { gson.fromJson(it, AnnualFeeRule::class.java) },
            sortOrder = sortOrder, createdAt = createdAt)
    }
    private fun Card.toEntity(): CardEntity {
        return CardEntity(id = id, bankId = bankId, name = name, last4 = last4, creditLimit = creditLimit, status = status,
            statementDay = statementDay, dueDayType = dueDayType, dueDay = dueDay, dueIntervalDays = dueIntervalDays,
            annualFeeEnabled = annualFeeEnabled, annualFeeStartDate = annualFeeStartDate?.toString(),
            annualFeeRuleJson = annualFeeRule?.let { gson.toJson(it) }, sortOrder = sortOrder, createdAt = createdAt)
    }
}
