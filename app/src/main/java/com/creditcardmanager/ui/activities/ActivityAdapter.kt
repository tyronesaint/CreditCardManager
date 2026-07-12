package com.creditcardmanager.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.databinding.ItemActivityBinding
import com.creditcardmanager.model.ActivityWithProgress

class ActivityAdapter : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {
    private var items: List<ActivityWithProgress> = emptyList()

    fun submitList(list: List<ActivityWithProgress>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ActivityWithProgress) {
            binding.tvActivityName.text = item.activity.name
            binding.tvProgress.text = when (item.activity.type) {
                com.creditcardmanager.model.enums.ActivityType.AMOUNT_TARGET -> "进度: ¥${String.format("%.2f", item.progress.currentAmount)} / ¥${String.format("%.2f", item.activity.targetAmount ?: 0.0)}"
                com.creditcardmanager.model.enums.ActivityType.COUNT_TARGET -> "进度: ${item.progress.currentCount} / ${item.activity.targetCount ?: 0}"
                com.creditcardmanager.model.enums.ActivityType.CASHBACK_RATE -> "返现: ¥${String.format("%.2f", item.progress.currentCashback)}"
                else -> if (item.progress.isAchieved) "已完成 ✓" else "进行中"
            }
            binding.tvCardName.text = item.cardName ?: item.bankName ?: ""
            binding.progressBar.max = 100
            binding.progressBar.progress = when (item.activity.type) {
                com.creditcardmanager.model.enums.ActivityType.AMOUNT_TARGET -> {
                    if (item.activity.targetAmount != null && item.activity.targetAmount > 0)
                        ((item.progress.currentAmount / item.activity.targetAmount) * 100).toInt().coerceIn(0, 100)
                    else 0
                }
                com.creditcardmanager.model.enums.ActivityType.COUNT_TARGET -> {
                    if (item.activity.targetCount != null && item.activity.targetCount > 0)
                        ((item.progress.currentCount.toDouble() / item.activity.targetCount) * 100).toInt().coerceIn(0, 100)
                    else 0
                }
                else -> if (item.progress.isAchieved) 100 else 0
            }
        }
    }
}
