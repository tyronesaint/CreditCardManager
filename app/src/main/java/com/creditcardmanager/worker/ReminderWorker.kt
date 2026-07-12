package com.creditcardmanager.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.creditcardmanager.data.repository.BankRepository
import com.creditcardmanager.data.repository.CardRepository
import com.creditcardmanager.data.repository.TransactionRepository
import com.creditcardmanager.utils.AppSettings
import com.creditcardmanager.utils.DateUtils
import com.creditcardmanager.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cardRepo: CardRepository,
    private val bankRepo: BankRepository,
    private val transactionRepo: TransactionRepository,
    private val appSettings: AppSettings,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val reminderDays = appSettings.getReminderDaysBefore()
        val reminderTime = appSettings.getReminderTimeString()

        val banks = bankRepo.getAllBanks().first()
        val cards = cardRepo.getActiveCards().first()
        val bankMap = banks.associateBy { it.id }

        for (card in cards) {
            val statementDate = DateUtils.getStatementDate(card.statementDay, today)
            val dueDate = card.getDueDateForStatement(statementDate)
            val daysUntil = ChronoUnit.DAYS.between(today, dueDate).toInt()

            if (daysUntil in reminderDays) {
                val statementPeriod = DateUtils.getStatementPeriod(card.statementDay, today)
                val amount = transactionRepo.getTotalAmountByCardAndDateRange(card.id, statementPeriod.first, statementPeriod.second)

                val title = if (daysUntil == 0) "今天还款！" else "还款提醒"
                val message = buildString {
                    append("${card.getDisplayName()}")
                    if (bankMap[card.bankId]?.shortName != null) {
                        append(" (${bankMap[card.bankId]?.shortName})")
                    }
                    append(" 还款日: $dueDate")
                    if (amount > 0) append(" 账单: ¥${String.format("%.2f", amount)}")
                }

                val notificationId = 1000 + card.hashCode()
                notificationHelper.showPaymentReminder(title, message, notificationId)
            }
        }

        return Result.success()
    }
}
