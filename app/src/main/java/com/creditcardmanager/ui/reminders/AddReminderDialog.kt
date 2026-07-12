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
import com.creditcardmanager.model.ReminderRepeatType
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

        val spinnerRepeat = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
                listOf("一次性", "每天", "每周", "每月几号", "每月几次"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editRepeatValue = EditText(requireContext()).apply {
            hint = "具体值（周几1-7 / 几号1-31 / 几次）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            visibility = android.view.View.GONE
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
        layout.addView(spinnerRepeat)
        layout.addView(editRepeatValue)
        layout.addView(editOffset)
        layout.addView(editTime)

        // 根据重复类型显示/隐藏具体值输入
        spinnerRepeat.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                when (position) {
                    0 -> { // 一次性
                        editRepeatValue.visibility = android.view.View.GONE
                    }
                    1 -> { // 每天
                        editRepeatValue.visibility = android.view.View.GONE
                    }
                    2 -> { // 每周
                        editRepeatValue.visibility = android.view.View.VISIBLE
                        editRepeatValue.hint = "周几（1=周一, 7=周日）"
                    }
                    3 -> { // 每月几号
                        editRepeatValue.visibility = android.view.View.VISIBLE
                        editRepeatValue.hint = "每月几号（1-31）"
                    }
                    4 -> { // 每月几次
                        editRepeatValue.visibility = android.view.View.VISIBLE
                        editRepeatValue.hint = "每月几次"
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("添加提醒")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val repeatType = when (spinnerRepeat.selectedItemPosition) {
                    0 -> ReminderRepeatType.ONCE
                    1 -> ReminderRepeatType.DAILY
                    2 -> ReminderRepeatType.WEEKLY
                    3 -> ReminderRepeatType.MONTHLY_DATE
                    4 -> ReminderRepeatType.MONTHLY_COUNT
                    else -> ReminderRepeatType.ONCE
                }

                val repeatValue = if (repeatType == ReminderRepeatType.WEEKLY ||
                                      repeatType == ReminderRepeatType.MONTHLY_DATE ||
                                      repeatType == ReminderRepeatType.MONTHLY_COUNT) {
                    editRepeatValue.text.toString().trim().takeIf { it.isNotEmpty() }
                } else null

                // 验证具体值
                if (repeatType == ReminderRepeatType.WEEKLY) {
                    val v = repeatValue?.toIntOrNull()
                    if (v == null || v !in 1..7) {
                        Toast.makeText(requireContext(), "周几必须是1-7", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }
                if (repeatType == ReminderRepeatType.MONTHLY_DATE) {
                    val v = repeatValue?.toIntOrNull()
                    if (v == null || v !in 1..31) {
                        Toast.makeText(requireContext(), "日期必须是1-31", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }
                if (repeatType == ReminderRepeatType.MONTHLY_COUNT) {
                    val v = repeatValue?.toIntOrNull()
                    if (v == null || v < 1) {
                        Toast.makeText(requireContext(), "次数必须大于0", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }

                val offset = editOffset.text.toString().toIntOrNull() ?: 0
                val time = editTime.text.toString().takeIf { it.isNotBlank() } ?: "09:00"
                val sourceType = if (spinnerType.selectedItemPosition == 0) SourceType.INDEPENDENT else SourceType.ACTIVITY

                val reminder = Reminder(
                    id = IdGenerator.generateReminderId(),
                    sourceType = sourceType,
                    title = title,
                    remindTimes = listOf(ReminderTime(offsetDays = offset, timeOfDay = time)),
                    repeatType = repeatType,
                    repeatValue = repeatValue,
                    enabled = true
                )
                onSave(reminder)
            }
            .setNegativeButton("取消", null)
            .create()
    }
}
