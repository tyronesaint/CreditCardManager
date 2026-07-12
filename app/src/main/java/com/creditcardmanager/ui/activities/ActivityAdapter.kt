package com.creditcardmanager.ui.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.R
import com.creditcardmanager.databinding.ItemActivityBinding
import com.creditcardmanager.model.ActivityWithProgress
import com.creditcardmanager.model.enums.ActivityType

class ActivityAdapter(
    private val onItemClick: ((ActivityWithProgress) -> Unit)? = null,
    private val onItemLongClick: ((ActivityWithProgress) -> Unit)? = null
) : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {
    private var items: List<ActivityWithProgress> = emptyList()

    fun submitList(list: List<ActivityWithProgress>) {
        items = list
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): ActivityWithProgress = items[position]

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
            binding.tvCardName.text = item.cardName ?: item.bankName ?: ""

            val colorRes = when (item.activity.type) {
                ActivityType.AMOUNT_TARGET -> R.color.act_amount
                ActivityType.COUNT_TARGET -> R.color.act_count
                ActivityType.CASHBACK_RATE -> R.color.act_rebate
                ActivityType.CONTINUOUS_PERIOD -> R.color.act_continuous
                ActivityType.FIRST_SPEND -> R.color.act_first
                ActivityType.CHECKIN_DAILY -> R.color.act_checkin
                ActivityType.CONSECUTIVE_DAYS -> R.color.act_continuous
                ActivityType.WEEKLY_CLAIM -> R.color.act_first
            }
            binding.vColorBar.setBackgroundColor(ContextCompat.getColor(binding.root.context, colorRes))

            binding.tvProgress.text = when (item.activity.type) {
                ActivityType.AMOUNT_TARGET -> "进度: ¥${String.format("%.2f", item.progress.currentAmount)} / ¥${String.format("%.2f", item.activity.targetAmount ?: 0.0)}"
                ActivityType.COUNT_TARGET -> "进度: ${item.progress.currentCount} / ${item.activity.targetCount ?: 0}"
                ActivityType.CASHBACK_RATE -> "返现: ¥${String.format("%.2f", item.progress.currentCashback)}"
                ActivityType.CONTINUOUS_PERIOD -> "连续达标: ${item.progress.continuousDone} / ${item.activity.requiredPeriods ?: 1}"
                ActivityType.FIRST_SPEND -> if (item.progress.isAchieved) "已完成 ✓" else "未首刷"
                ActivityType.CHECKIN_DAILY -> if (item.progress.isAchieved) "今日已签到 ✓" else "今日未签到"
                ActivityType.CONSECUTIVE_DAYS -> "已连续消费 ${item.progress.currentCount} / ${item.activity.targetCount ?: 0} 天"
                ActivityType.WEEKLY_CLAIM -> if (item.progress.isAchieved) "本周已领取 ✓" else "本周未领取"
            }

            binding.progressBar.max = 100
            binding.progressBar.progress = when (item.activity.type) {
                ActivityType.AMOUNT_TARGET -> {
                    if (item.activity.targetAmount != null && item.activity.targetAmount > 0)
                        ((item.progress.currentAmount / item.activity.targetAmount) * 100).toInt().coerceIn(0, 100)
                    else 0
                }
                ActivityType.COUNT_TARGET -> {
                    if (item.activity.targetCount != null && item.activity.targetCount > 0)
                        ((item.progress.currentCount.toDouble() / item.activity.targetCount) * 100).toInt().coerceIn(0, 100)
                    else 0
                }
                ActivityType.CONTINUOUS_PERIOD -> {
                    if (item.activity.requiredPeriods != null && item.activity.requiredPeriods > 0)
                        ((item.progress.continuousDone.toDouble() / item.activity.requiredPeriods) * 100).toInt().coerceIn(0, 100)
                    else 0
                }
                ActivityType.CONSECUTIVE_DAYS -> {
                    if (item.activity.targetCount != null && item.activity.targetCount > 0)
                        ((item.progress.currentCount.toDouble() / item.activity.targetCount) * 100).toInt().coerceIn(0, 100)
                    else 0
                }
                else -> if (item.progress.isAchieved) 100 else 0
            }

            if (item.activity.type == ActivityType.CASHBACK_RATE) {
                val monthlyCap = item.activity.monthlyCap
                val dailyCap = item.activity.dailyCap
                val rebateText = buildString {
                    if (monthlyCap != null && monthlyCap > 0) {
                        append("本月已返 ¥${String.format("%.2f", item.progress.currentCashback)} / 上限 ¥${String.format("%.2f", monthlyCap)}")
                    }
                    if (dailyCap != null && dailyCap > 0) {
                        if (isNotEmpty()) append("  •  ")
                        append("今日已返 ¥${String.format("%.2f", item.progress.todayCashback)} / 上限 ¥${String.format("%.2f", dailyCap)}")
                    }
                }
                if (rebateText.isNotEmpty()) {
                    binding.tvRebateInfo.text = rebateText
                    binding.tvRebateInfo.visibility = View.VISIBLE
                } else {
                    binding.tvRebateInfo.visibility = View.GONE
                }
            } else {
                binding.tvRebateInfo.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick?.invoke(item) }
            binding.root.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                true
            }
        }
    }
}
