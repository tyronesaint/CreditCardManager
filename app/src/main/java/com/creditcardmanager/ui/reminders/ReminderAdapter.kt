package com.creditcardmanager.ui.reminders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.databinding.ItemReminderBinding
import com.creditcardmanager.model.Reminder

class ReminderAdapter(
    private val onToggle: (Reminder, Boolean) -> Unit,
    private val onComplete: (Reminder) -> Unit,
    private val onDelete: (Reminder) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ViewHolder>() {
    private var items: List<Reminder> = emptyList()

    fun submitList(list: List<Reminder>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reminder: Reminder) {
            binding.tvTitle.text = reminder.title
            binding.tvTime.text = reminder.remindTimes.joinToString { "${it.offsetDays}天后 ${it.timeOfDay}" }
            binding.switchEnabled.isChecked = reminder.enabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(reminder, isChecked)
            }
            binding.cbCompleted.isChecked = reminder.completed
            binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) onComplete(reminder)
            }
            binding.btnDelete.setOnClickListener { onDelete(reminder) }

            if (reminder.completed) {
                binding.tvTitle.alpha = 0.5f
                binding.tvTime.alpha = 0.5f
            } else {
                binding.tvTitle.alpha = 1f
                binding.tvTime.alpha = 1f
            }
        }
    }
}
