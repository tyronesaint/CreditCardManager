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
import com.creditcardmanager.utils.IdGenerator
import com.creditcardmanager.viewmodel.ActivityViewModel
import com.creditcardmanager.viewmodel.BankViewModel
import com.creditcardmanager.viewmodel.CardViewModel
import kotlinx.coroutines.launch

class AddActivityDialog(
    private val bankViewModel: BankViewModel,
    private val cardViewModel: CardViewModel,
    private val activityViewModel: ActivityViewModel
) : DialogFragment() {

    private var banks: List<Bank> = emptyList()
    private var cards: List<Card> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val editName = EditText(requireContext()).apply {
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
                listOf("金额达标", "笔数达标", "比例返现", "连续达标", "首刷奖", "每日签到")
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val spinnerPeriod = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf("自然月", "自然周", "自然日", "一次性")
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editTarget = EditText(requireContext()).apply {
            hint = "目标金额/笔数/比例"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editRequiredPeriods = EditText(requireContext()).apply {
            hint = "连续几期"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            visibility = View.GONE
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bankViewModel.banks.collect { bankList ->
                    banks = bankList
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
                if (type == ActivityType.CONTINUOUS_PERIOD) {
                    editRequiredPeriods.visibility = View.VISIBLE
                    editTarget.hint = "每期目标金额/笔数"
                } else {
                    editRequiredPeriods.visibility = View.GONE
                    editTarget.hint = when (type) {
                        ActivityType.AMOUNT_TARGET -> "目标金额"
                        ActivityType.COUNT_TARGET -> "目标笔数"
                        ActivityType.CASHBACK_RATE -> "返现比例(%)"
                        else -> "目标金额/笔数"
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("添加活动")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val name = editName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入活动名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val level = if (spinnerLevel.selectedItemPosition == 0) ActivityLevel.BANK else ActivityLevel.CARD
                val targetPos = spinnerTarget.selectedItemPosition
                val bankId = if (level == ActivityLevel.BANK) banks.getOrNull(targetPos)?.id else null
                val cardId = if (level == ActivityLevel.CARD) cards.getOrNull(targetPos)?.id else null

                if (level == ActivityLevel.BANK && bankId == null) {
                    Toast.makeText(requireContext(), "请先添加银行", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (level == ActivityLevel.CARD && cardId == null) {
                    Toast.makeText(requireContext(), "请先添加卡片", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val type = ActivityType.values()[spinnerType.selectedItemPosition]

                val periodType = when (spinnerPeriod.selectedItemPosition) {
                    0 -> PeriodType.NATURAL_MONTH
                    1 -> PeriodType.NATURAL_WEEK
                    2 -> PeriodType.NATURAL_DAY
                    else -> PeriodType.ONE_TIME
                }

                val targetValue = editTarget.text.toString().toDoubleOrNull()
                val requiredPeriods = if (type == ActivityType.CONTINUOUS_PERIOD) {
                    editRequiredPeriods.text.toString().toIntOrNull() ?: 1
                } else null

                val activity = Activity(
                    id = IdGenerator.generateActivityId(),
                    name = name,
                    level = level,
                    bankId = bankId,
                    cardId = cardId,
                    type = type,
                    periodType = periodType,
                    targetAmount = if (type == ActivityType.AMOUNT_TARGET) targetValue else null,
                    targetCount = if (type == ActivityType.COUNT_TARGET) targetValue?.toInt() else null,
                    cashbackRate = if (type == ActivityType.CASHBACK_RATE) targetValue?.let { it / 100 } else null,
                    requiredPeriods = requiredPeriods,
                    filter = ActivityFilter(),
                    reward = ActivityReward(rewardType = RewardType.NONE)
                )
                activityViewModel.addActivity(activity)
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
    }
}