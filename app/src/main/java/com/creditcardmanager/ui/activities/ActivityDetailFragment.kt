package com.creditcardmanager.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.creditcardmanager.databinding.FragmentCardDetailBinding
import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.enums.ActivityType
import com.creditcardmanager.viewmodel.ActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ActivityDetailFragment : Fragment() {
    private var _binding: FragmentCardDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by viewModels()
    private val args: ActivityDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectActivity(args.activityId)
        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedActivity.collect { detail ->
                    detail?.let { updateUI(it) }
                }
            }
        }
    }

    private fun updateUI(detail: ActivityViewModel.ActivityDetail) {
        val activity = detail.activity
        binding.tvCardName.text = activity.name
        binding.tvBankName.text = detail.bankName ?: detail.cardName ?: "通用活动"
        binding.tvCreditLimit.text = "类型: ${activity.type.name}"
        binding.tvStatementDay.text = "周期: ${activity.periodType.name}"
        binding.tvDueDate.text = "目标: ${getTargetText(activity)}"
        binding.tvInterestFree.text = "进度: ${getProgressText(detail)}"
        binding.tvStatementAmount.text = "¥${String.format("%.2f", detail.progress.currentAmount)}"
        binding.tvAnnualFee.text = buildString {
            if (activity.isArchived) append("[已归档] ")
            append("长按编辑或删除")
        }

        binding.root.setOnLongClickListener {
            showActionDialog(activity)
            true
        }
    }

    private fun getTargetText(activity: Activity): String {
        return when (activity.type) {
            ActivityType.AMOUNT_TARGET -> "消费满 ¥${activity.targetAmount ?: 0}"
            ActivityType.COUNT_TARGET -> "消费 ${activity.targetCount ?: 0} 笔"
            ActivityType.CASHBACK_RATE -> "返现比例 ${(activity.cashbackRate ?: 0.0) * 100}%"
            ActivityType.CONTINUOUS_PERIOD -> "连续 ${activity.requiredPeriods ?: 1} 期达标"
            ActivityType.FIRST_SPEND -> "首笔消费满 ¥${activity.minAmount ?: 0}"
            ActivityType.CHECKIN_DAILY -> "每日签到"
            ActivityType.CONSECUTIVE_DAYS -> "连续消费 ${activity.targetCount ?: 0} 天"
            ActivityType.WEEKLY_CLAIM -> "每周固定日期领取"
        }
    }

    private fun getProgressText(detail: ActivityViewModel.ActivityDetail): String {
        val p = detail.progress
        return when (detail.activity.type) {
            ActivityType.AMOUNT_TARGET -> "¥${String.format("%.2f", p.currentAmount)} / ¥${String.format("%.2f", detail.activity.targetAmount ?: 0.0)}"
            ActivityType.COUNT_TARGET -> "${p.currentCount} / ${detail.activity.targetCount ?: 0} 笔"
            ActivityType.CASHBACK_RATE -> "已返 ¥${String.format("%.2f", p.currentCashback)}"
            ActivityType.CONTINUOUS_PERIOD -> "连续 ${p.continuousDone} / ${detail.activity.requiredPeriods ?: 1} 期"
            ActivityType.FIRST_SPEND -> if (p.isAchieved) "已完成" else "未完成"
            ActivityType.CHECKIN_DAILY -> if (p.isAchieved) "今日已签" else "今日未签"
            ActivityType.CONSECUTIVE_DAYS -> "已连续 ${p.currentCount} / ${detail.activity.targetCount ?: 0} 天"
            ActivityType.WEEKLY_CLAIM -> if (p.isAchieved) "本周已领取" else "本周未领取"
        }
    }

    private fun showActionDialog(activity: Activity) {
        val options = if (activity.isArchived) {
            arrayOf("恢复活动", "彻底删除")
        } else {
            arrayOf("编辑活动", "归档活动", "删除活动")
        }
        AlertDialog.Builder(requireContext())
            .setTitle(activity.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "编辑活动" -> showEditDialog(activity)
                    "归档活动" -> viewModel.archiveActivity(activity.id)
                    "恢复活动" -> viewModel.unarchiveActivity(activity.id)
                    "删除活动", "彻底删除" -> confirmDelete(activity)
                }
            }
            .show()
    }

    private fun confirmDelete(activity: Activity) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除后将无法恢复，确定删除「${activity.name}」？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteActivity(activity)
                findNavController().popBackStack()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditDialog(activity: Activity) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val editName = EditText(requireContext()).apply {
            setText(activity.name)
            hint = "活动名称"
        }
        val editTarget = EditText(requireContext()).apply {
            val value = when (activity.type) {
                ActivityType.AMOUNT_TARGET -> activity.targetAmount?.toString() ?: ""
                ActivityType.COUNT_TARGET -> activity.targetCount?.toString() ?: ""
                ActivityType.CASHBACK_RATE -> activity.cashbackRate?.let { (it * 100).toString() } ?: ""
                ActivityType.CONSECUTIVE_DAYS -> activity.targetCount?.toString() ?: ""
                else -> ""
            }
            setText(value)
            hint = "目标值"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(editName)
        layout.addView(editTarget)

        AlertDialog.Builder(requireContext())
            .setTitle("编辑活动")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val newName = editName.text.toString().trim()
                val newTarget = editTarget.text.toString().toDoubleOrNull()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updated = activity.copy(
                    name = newName,
                    targetAmount = if (activity.type == ActivityType.AMOUNT_TARGET) newTarget else activity.targetAmount,
                    targetCount = if (activity.type == ActivityType.COUNT_TARGET || activity.type == ActivityType.CONSECUTIVE_DAYS) newTarget?.toInt() else activity.targetCount,
                    cashbackRate = if (activity.type == ActivityType.CASHBACK_RATE) newTarget?.let { it / 100 } else activity.cashbackRate
                )
                viewModel.updateActivity(updated)
                Toast.makeText(requireContext(), "已更新", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
