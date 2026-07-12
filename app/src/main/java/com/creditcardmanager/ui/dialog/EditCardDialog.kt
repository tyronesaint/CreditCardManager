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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.creditcardmanager.model.Bank
import com.creditcardmanager.model.Card
import com.creditcardmanager.model.enums.DueDayType
import com.creditcardmanager.viewmodel.BankViewModel
import kotlinx.coroutines.launch

class EditCardDialog(
    private val card: Card,
    private val bankViewModel: BankViewModel,
    private val onSave: (Card) -> Unit
) : DialogFragment() {

    private var banks: List<Bank> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val spinnerBank = Spinner(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editName = EditText(requireContext()).apply {
            setText(card.name)
            hint = "卡片名称"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editLast4 = EditText(requireContext()).apply {
            setText(card.last4 ?: "")
            hint = "卡号后四位（选填）"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editLimit = EditText(requireContext()).apply {
            card.creditLimit?.let { setText(String.format("%.0f", it)) }
            hint = "信用额度（选填）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editStatementDay = EditText(requireContext()).apply {
            setText(card.statementDay.toString())
            hint = "账单日（1-28）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val spinnerDueType = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
                listOf("固定日期", "固定间隔"))
            setSelection(if (card.dueDayType == DueDayType.FIXED_DATE) 0 else 1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editDueDay = EditText(requireContext()).apply {
            card.dueDay?.let { setText(it.toString()) }
            card.dueIntervalDays?.let { setText(it.toString()) }
            hint = "还款日（1-28）或间隔天数"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        layout.addView(spinnerBank)
        layout.addView(editName)
        layout.addView(editLast4)
        layout.addView(editLimit)
        layout.addView(editStatementDay)
        layout.addView(spinnerDueType)
        layout.addView(editDueDay)

        // Load bank list and select current bank
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bankViewModel.banks.collect { bankList ->
                    banks = bankList
                    spinnerBank.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        bankList.map { it.name }
                    )
                    // Select current bank
                    val currentIndex = bankList.indexOfFirst { it.id == card.bankId }
                    if (currentIndex >= 0) {
                        spinnerBank.setSelection(currentIndex)
                    }
                }
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("编辑卡片")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val bankPos = spinnerBank.selectedItemPosition
                val bankId = banks.getOrNull(bankPos)?.id ?: card.bankId
                val name = editName.text.toString().trim()
                val statementDay = editStatementDay.text.toString().toIntOrNull()
                val dueDay = editDueDay.text.toString().toIntOrNull()
                val limit = editLimit.text.toString().toDoubleOrNull()
                val last4 = editLast4.text.toString().trim().takeIf { it.isNotEmpty() }

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入卡片名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (statementDay == null || statementDay !in 1..28) {
                    Toast.makeText(requireContext(), "账单日必须是1-28", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (dueDay == null || dueDay !in 1..28) {
                    Toast.makeText(requireContext(), "还款日/间隔天数必须是1-28", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val dueDayType = if (spinnerDueType.selectedItemPosition == 0) DueDayType.FIXED_DATE else DueDayType.FIXED_INTERVAL

                val updated = card.copy(
                    bankId = bankId,
                    name = name,
                    last4 = last4,
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
