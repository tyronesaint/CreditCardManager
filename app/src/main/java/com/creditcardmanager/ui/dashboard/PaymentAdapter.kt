package com.creditcardmanager.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.databinding.ItemPaymentBinding
import com.creditcardmanager.model.PaymentDue

class PaymentAdapter : RecyclerView.Adapter<PaymentAdapter.ViewHolder>() {
    private var items: List<PaymentDue> = emptyList()

    fun submitList(list: List<PaymentDue>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemPaymentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(payment: PaymentDue) {
            binding.tvCardName.text = payment.cardName
            binding.tvDueDate.text = "还款日: ${payment.dueDate}"
            binding.tvDaysRemaining.text = "${payment.daysRemaining}天后"
            binding.tvAmount.text = "¥${String.format("%.2f", payment.statementAmount)}"
        }
    }
}
