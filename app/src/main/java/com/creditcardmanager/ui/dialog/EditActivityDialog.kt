package com.creditcardmanager.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
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
import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.ActivityFilter
import com.creditcardmanager.model.ActivityReward
import com.creditcardmanager.model.Bank
import com.creditcardmanager.model.Card
import com.creditcardmanager.model.enums.ActivityLevel
import com.creditcardmanager.model.enums.ActivityType
import com.creditcardmanager.model.enums.PeriodType
import com.creditcardmanager.model.enums.RewardType
import com.creditcardmanager.viewmodel.ActivityViewModel
import com.creditcardmanager.viewmodel.BankViewModel
import com.creditcardmanager.viewmodel.CardViewModel
import kotlinx.coroutines.launch

class EditActivityDialog(
    private val activity: Activity,
    private val bankViewModel: BankViewModel,
    private val cardViewModel: CardViewModel,
    private val activityViewModel: ActivityViewModel,
    private val onSaved: (() -> Unit)? = null
) : DialogFragment() {

    private var banks: List<Bank> = emptyList()
    private var cards: List<Card> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val editName = EditText(requireContext()).apply {
            setText(activity.name)
            hint = "活动名称"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val spinnerLevel = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf("银行级活动", "卡片级活动")
            )
            setSelection(if (activity.level == ActivityLevel.BANK) 0 else 1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val spinnerTarget = Spinner(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val spinnerType = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ActivityType.values().map { it.toDisplayName() }
            )
            setSelection(ActivityType.values().indexOf(activity.type))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val spinnerPeriod = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf("自然月", "自然周", "自然日", "绑定账单周期", "一次性")
            )
            setSelection(when (activity.periodType) {
                PeriodType.NATURAL_MONTH -> 0
                PeriodType.NATURAL_WEEK -> 1
                PeriodType.NATURAL_DAY -> 2
                PeriodType.BIND_STATEMENT -> 3
                else -> 4
            })
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editTarget = EditText(requireContext()).apply {
            val value = when (activity.type) {
                ActivityType.AMOUNT_TARGET -> activity.targetAmount?.toString() ?: ""
                ActivityType.COUNT_TARGET -> activity.targetCount?.toString() ?: ""
                ActivityType.CASHBACK_RATE -> activity.cashbackRate?.let { (it * 100).toString() } ?: ""
                ActivityType.FIRST_SPEND -> activity.minAmount?.toString() ?: ""
                ActivityType.CONSECUTIVE_DAYS -> activity.targetCount?.toString() ?: ""
                else -> ""
            }
            setText(value)
            hint = "目标金额/笔数/比例"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editRequiredPeriods = EditText(requireContext()).apply {
            setText(activity.requiredPeriods?.toString() ?: "")
            hint = "连续几期/几天（选填）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            visibility = if (activity.type == ActivityType.CONTINUOUS_PERIOD || activity.type == ActivityType.CONSECUTIVE_DAYS) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editMinPerAmount = EditText(requireContext()).apply {
            setText(activity.minPerAmount?.toString() ?: "")
            hint = "每笔最低金额（选填）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            visibility = if (activity.type == ActivityType.COUNT_TARGET || activity.type == ActivityType.CONSECUTIVE_DAYS) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editDailyCap = EditText(requireContext()).apply {
            setText(activity.dailyCap?.toString() ?: "")
            hint = "每日返现上限（选填）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            visibility = if (activity.type == ActivityType.CASHBACK_RATE) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editMonthlyCap = EditText(requireContext()).apply {
            setText(activity.monthlyCap?.toString() ?: "")
            hint = "每月返现上限（选填）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            visibility = if (activity.type == ActivityType.CASHBACK_RATE) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editDescription = EditText(requireContext()).apply {
            setText(activity.description ?: "")
            hint = "活动描述（选填）"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        layout.addView(editName)
        layout.addView(spinnerLevel)
        layout.addView(spinnerTarget)
        layout.addView(spinnerType)
        layout.addView(spinnerPeriod)
        layout.addView(editTarget)
        layout.addView(editRequiredPeriods)
        layout.addView(editMinPerAmount)
        layout.addView(editDailyCap)
        layout.addView(editMonthlyCap)
        layout.addView(editDescription)

        // Load banks and cards
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bankViewModel.banks.collect { bankList ->
                    banks = bankList
                    updateTargetSpinner(spinnerLevel, spinnerTarget)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cardViewModel.cards.collect { cardPairs ->
                    cards = cardPairs.flatMap { it.second }
                    updateTargetSpinner(spinnerLevel, spinnerTarget)
                }
            }
        }

        spinnerLevel.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateTargetSpinner(spinnerLevel, spinnerTarget)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        spinnerType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val type = ActivityType.values()[position]
                when (type) {
                    ActivityType.CONTINUOUS_PERIOD -> {
                        editRequiredPeriods.visibility = View.VISIBLE
                        editRequiredPeriods.hint = "连续几期达标"
                        editTarget.hint = "每期目标金额/笔数"
                        editMinPerAmount.visibility = View.VISIBLE
                        editDailyCap.visibility = View.GONE
                        editMonthlyCap.visibility = View.GONE
                    }
                    ActivityType.CONSECUTIVE_DAYS -> {
                        editRequiredPeriods.visibility = View.VISIBLE
                        editRequiredPeriods.hint = "连续消费N天"
                        editTarget.hint = "目标天数"
                        editMinPerAmount.visibility = View.VISIBLE
                        editDailyCap.visibility = View.GONE
                        editMonthlyCap.visibility = View.GONE
                    }
                    ActivityType.CASHBACK_RATE -> {
                        editRequiredPeriods.visibility = View.GONE
                        editTarget.hint = "返现比例(%)"
                        editMinPerAmount.visibility = View.GONE
                        editDailyCap.visibility = View.VISIBLE
                        editMonthlyCap.visibility = View.VISIBLE
                    }
                    ActivityType.FIRST_SPEND -> {
                        editRequiredPeriods.visibility = View.GONE
                        editTarget.hint = "首刷最低金额"
                        editMinPerAmount.visibility = View.GONE
                        editDailyCap.visibility = View.GONE
                        editMonthlyCap.visibility = View.GONE
                    }
                    else -> {
                        editRequiredPeriods.visibility = View.GONE
                        editMinPerAmount.visibility = if (type == ActivityType.COUNT_TARGET) View.VISIBLE else View.GONE
                        editDailyCap.visibility = View.GONE
                        editMonthlyCap.visibility = View.GONE
                        editTarget.hint = when (type) {
                            ActivityType.AMOUNT_TARGET -> "目标金额"
                            ActivityType.COUNT_TARGET -> "目标笔数"
                            else -> "目标值"
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("编辑活动")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val name = editName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入活动名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val levelPos = spinnerLevel.selectedItemPosition
                val level = if (levelPos == 0) ActivityLevel.BANK else ActivityLevel.CARD
                val targetPos = spinnerTarget.selectedItemPosition
                val bankId = if (level == ActivityLevel.BANK) banks.getOrNull(targetPos)?.id else null
                val cardId = if (level == ActivityLevel.CARD) cards.getOrNull(targetPos)?.id else null

                val type = ActivityType.values()[spinnerType.selectedItemPosition]
                val periodType = when (spinnerPeriod.selectedItemPosition) {
                    0 -> PeriodType.NATURAL_MONTH
                    1 -> PeriodType.NATURAL_WEEK
                    2 -> PeriodType.NATURAL_DAY
                    3 -> PeriodType.BIND_STATEMENT
                    else -> PeriodType.ONE_TIME
                }

                val targetValue = editTarget.text.toString().toDoubleOrNull()
                val requiredPeriods = editRequiredPeriods.text.toString().toIntOrNull()
                val minPerAmount = editMinPerAmount.text.toString().toDoubleOrNull()
                val dailyCap = editDailyCap.text.toString().toDoubleOrNull()
                val monthlyCap = editMonthlyCap.text.toString().toDoubleOrNull()
                val description = editDescription.text.toString().takeIf { it.isNotBlank() }

                val updated = activity.copy(
                    name = name,
                    level = level,
                    bankId = bankId,
                    cardId = cardId,
                    type = type,
                    periodType = periodType,
                    description = description,
                    targetAmount = if (type == ActivityType.AMOUNT_TARGET || type == ActivityType.CONTINUOUS_PERIOD) targetValue else null,
                    targetCount = if (type == ActivityType.COUNT_TARGET || type == ActivityType.CONSECUTIVE_DAYS) targetValue?.toInt() else null,
                    minPerAmount = if (type == ActivityType.COUNT_TARGET || type == ActivityType.CONSECUTIVE_DAYS) minPerAmount else null,
                    cashbackRate = if (type == ActivityType.CASHBACK_RATE) targetValue?.let { it / 100 } else null,
                    dailyCap = if (type == ActivityType.CASHBACK_RATE) dailyCap else null,
                    monthlyCap = if (type == ActivityType.CASHBACK_RATE) monthlyCap else null,
                    requiredPeriods = if (type == ActivityType.CONTINUOUS_PERIOD) requiredPeriods else null,
                    minAmount = if (type == ActivityType.FIRST_SPEND) targetValue else null
                )
                activityViewModel.updateActivity(updated)
                Toast.makeText(requireContext(), "已更新", Toast.LENGTH_SHORT).show()
                onSaved?.invoke()
            }
            .setNegativeButton("取消", null)
            .create()
    }

    private fun updateTargetSpinner(levelSpinner: Spinner, targetSpinner: Spinner) {
        val isBank = levelSpinner.selectedItemPosition == 0
        val items = if (isBank) banks.map { it.name } else cards.map { it.getDisplayName() }
        targetSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            items
        )
        // Try to select current target
        if (isBank && activity.bankId != null) {
            val index = banks.indexOfFirst { it.id == activity.bankId }
            if (index >= 0) targetSpinner.setSelection(index)
        } else if (!isBank && activity.cardId != null) {
            val index = cards.indexOfFirst { it.id == activity.cardId }
            if (index >= 0) targetSpinner.setSelection(index)
        }
    }

    private fun ActivityType.toDisplayName(): String = when (this) {
        ActivityType.AMOUNT_TARGET -> "金额达标"
        ActivityType.COUNT_TARGET -> "笔数达标"
        ActivityType.CASHBACK_RATE -> "比例返现"
        ActivityType.CONTINUOUS_PERIOD -> "连续达标"
        ActivityType.FIRST_SPEND -> "首刷奖"
        ActivityType.CHECKIN_DAILY -> "每日签到"
        ActivityType.CONSECUTIVE_DAYS -> "连续消费N天"
        ActivityType.WEEKLY_CLAIM -> "每周固定领取"
    }
}
