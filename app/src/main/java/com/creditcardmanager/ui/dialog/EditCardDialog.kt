package com.creditcardmanager.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.creditcardmanager.model.Card
import com.creditcardmanager.model.enums.DueDayType

class EditCardDialog(
    private val card: Card,
    private val onSave: (Card) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val editName = EditText(requireContext()).apply {
            setText(card.name)
            hint = "卡片名称"
        }

        val editLimit = EditText(requireContext()).apply {
            card.creditLimit?.let { setText(String.format("%.0f", it)) }
            hint = "信用额度"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val editStatementDay = EditText(requireContext()).apply {
            setText(card.statementDay.toString())
            hint = "账单日（1-28）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val spinnerDueType = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
                listOf("固定日期", "固定间隔"))
            setSelection(if (card.dueDayType == DueDayType.FIXED_DATE) 0 else 1)
        }

        val editDueDay = EditText(requireContext()).apply {
            card.dueDay?.let { setText(it.toString()) }
            card.dueIntervalDays?.let { setText(it.toString()) }
            hint = "还款日（1-28）或间隔天数"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(editName)
        layout.addView(editLimit)
        layout.addView(editStatementDay)
        layout.addView(spinnerDueType)
        layout.addView(editDueDay)

        return AlertDialog.Builder(requireContext())
            .setTitle("编辑卡片")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val name = editName.text.toString().trim()
                val statementDay = editStatementDay.text.toString().toIntOrNull()
                val dueDay = editDueDay.text.toString().toIntOrNull()
                val limit = editLimit.text.toString().toDoubleOrNull()

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入卡片名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (statementDay == null || statementDay !in 1..28) {
                    Toast.makeText(requireContext(), "账单日必须是1-28", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val dueDayType = if (spinnerDueType.selectedItemPosition == 0) DueDayType.FIXED_DATE else DueDayType.FIXED_INTERVAL

                val updated = card.copy(
                    name = name,
                    creditLimit = limit,
                    statementDay = statementDay,
                    dueDayType = dueDayType,
                    dueDay = if (dueDayType == DueDayType.FIXED_DATE) dueDay else null,
                    dueIntervalDays = if (dueDayType == DueDayType.FIXED_INTERVAL) dueDay else null
                )
                onSave(updated)
            }
            .setNegativeButton("取消", null)
            .create()
    }
}
