package com.creditcardmanager.ui.reminders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.R
import com.creditcardmanager.databinding.ItemPaymentReminderBinding
import com.creditcardmanager.model.PaymentDue

class PaymentReminderAdapter(
    private val onMarkPaid: (PaymentDue) -> Unit,
    private val onItemClick: ((PaymentDue) -> Unit)? = null
) : RecyclerView.Adapter<PaymentReminderAdapter.ViewHolder>() {
    private var items: List<PaymentDue> = emptyList()

    fun submitList(list: List<PaymentDue>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemPaymentReminderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(payment: PaymentDue) {
            binding.tvCardName.text = payment.cardName
            binding.tvBankName.text = payment.bankShortName ?: ""
            binding.tvDueDate.text = "还款日: ${payment.dueDate}"
            binding.tvAmount.text = "¥${String.format("%.2f", payment.statementAmount)}"

            val daysText = when {
                payment.daysRemaining == 0 -> "今天到期"
                payment.daysRemaining == 1 -> "明天到期"
                payment.daysRemaining < 0 -> "已逾期 ${-payment.daysRemaining} 天"
                else -> "${payment.daysRemaining} 天后到期"
            }
            binding.tvDaysRemaining.text = daysText

            val colorRes = when {
                payment.daysRemaining <= 0 -> R.color.error
                payment.daysRemaining <= 3 -> R.color.warning
                else -> R.color.success
            }
            binding.tvDaysRemaining.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))

            binding.btnMarkPaid.setOnClickListener { onMarkPaid(payment) }
            binding.root.setOnClickListener { onItemClick?.invoke(payment) }
        }
    }
}
