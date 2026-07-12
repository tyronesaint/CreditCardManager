package com.creditcardmanager.ui.reminders

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.creditcardmanager.model.Reminder
import com.creditcardmanager.model.ReminderTime
import com.creditcardmanager.model.enums.SourceType
import com.creditcardmanager.utils.IdGenerator

class AddReminderDialog(
    private val onSave: (Reminder) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val editTitle = EditText(requireContext()).apply {
            hint = "提醒标题（如：建行社保卡周三领券）"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val spinnerType = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
                listOf("独立提醒", "活动提醒"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editOffset = EditText(requireContext()).apply {
            hint = "提前几天提醒（0=当天）"
            setText("0")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editTime = EditText(requireContext()).apply {
            hint = "提醒时间（如 09:00）"
            setText("09:00")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        layout.addView(editTitle)
        layout.addView(spinnerType)
        layout.addView(editOffset)
        layout.addView(editTime)

        return AlertDialog.Builder(requireContext())
            .setTitle("添加提醒")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val offset = editOffset.text.toString().toIntOrNull() ?: 0
                val time = editTime.text.toString().takeIf { it.isNotBlank() } ?: "09:00"
                val sourceType = if (spinnerType.selectedItemPosition == 0) SourceType.INDEPENDENT else SourceType.ACTIVITY

                val reminder = Reminder(
                    id = IdGenerator.generateReminderId(),
                    sourceType = sourceType,
                    title = title,
                    remindTimes = listOf(ReminderTime(offsetDays = offset, timeOfDay = time)),
                    enabled = true
                )
                onSave(reminder)
            }
            .setNegativeButton("取消", null)
            .create()
    }
}
