package com.creditcardmanager.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.creditcardmanager.viewmodel.BankViewModel

class AddBankDialog(private val viewModel: BankViewModel) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val editName = EditText(requireContext()).apply {
            hint = "银行名称（如：工商银行）"
        }
        val editShort = EditText(requireContext()).apply {
            hint = "简称（如：工行，选填）"
        }
        layout.addView(editName)
        layout.addView(editShort)

        return AlertDialog.Builder(requireContext())
            .setTitle("添加银行")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val name = editName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addBank(name, editShort.text.toString().trim().takeIf { it.isNotEmpty() })
                }
            }
            .setNegativeButton("取消", null)
            .create()
    }
}
