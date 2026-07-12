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
import com.creditcardmanager.model.enums.CardStatus
import com.creditcardmanager.model.enums.DueDayType
import com.creditcardmanager.utils.IdGenerator
import com.creditcardmanager.viewmodel.BankViewModel
import com.creditcardmanager.viewmodel.CardViewModel
import kotlinx.coroutines.launch

class AddCardDialog(
    private val bankViewModel: BankViewModel,
    private val cardViewModel: CardViewModel
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
            hint = "卡片名称"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editLast4 = EditText(requireContext()).apply {
            hint = "卡号后四位（选填）"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editLimit = EditText(requireContext()).apply {
            hint = "信用额度（选填）"
            inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editStatementDay = EditText(requireContext()).apply {
            hint = "账单日（1-28）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editDueDay = EditText(requireContext()).apply {
            hint = "还款日（1-28，固定日期模式）"
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
        layout.addView(editDueDay)

        // 加载银行列表
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bankViewModel.banks.collect { bankList ->
                    banks = bankList
                    spinnerBank.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        bankList.map { it.name }
                    )
                }
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("添加信用卡")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val bankPos = spinnerBank.selectedItemPosition
                val bankId = banks.getOrNull(bankPos)?.id
                val name = editName.text.toString().trim()
                val statementDay = editStatementDay.text.toString().toIntOrNull()
                val dueDay = editDueDay.text.toString().toIntOrNull()
                val limit = editLimit.text.toString().toDoubleOrNull()

                if (bankId == null) {
                    Toast.makeText(requireContext(), "请先添加银行", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入卡片名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (statementDay == null || statementDay !in 1..28) {
                    Toast.makeText(requireContext(), "账单日必须是1-28", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (dueDay == null || dueDay !in 1..28) {
                    Toast.makeText(requireContext(), "还款日必须是1-28", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val card = Card(
                    id = IdGenerator.generateCardId(),
                    bankId = bankId,
                    name = name,
                    last4 = editLast4.text.toString().trim().takeIf { it.isNotEmpty() },
                    creditLimit = limit,
                    status = CardStatus.ACTIVE,
                    statementDay = statementDay,
                    dueDayType = DueDayType.FIXED_DATE,
                    dueDay = dueDay
                )
                cardViewModel.addCard(card)
            }
            .setNegativeButton("取消", null)
            .create()
    }
}
