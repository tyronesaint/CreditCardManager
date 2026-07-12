package com.creditcardmanager.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.databinding.ItemTopCardBinding
import com.creditcardmanager.model.InterestFreeInfo

class TopCardAdapter : RecyclerView.Adapter<TopCardAdapter.ViewHolder>() {
    private var items: List<InterestFreeInfo> = emptyList()

    fun submitList(list: List<InterestFreeInfo>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemTopCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(info: InterestFreeInfo) {
            binding.tvCardName.text = "${info.bankShortName ?: ""} ${info.cardName}".trim()
            binding.tvDueDate.text = "还款日: ${info.dueDate}"
            binding.tvDays.text = info.interestFreeDays.toString()
        }
    }
}
