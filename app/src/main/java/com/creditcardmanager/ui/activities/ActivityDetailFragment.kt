package com.creditcardmanager.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.creditcardmanager.R
import com.creditcardmanager.databinding.FragmentCardDetailBinding
import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.enums.ActivityType
import com.creditcardmanager.ui.dialog.EditActivityDialog
import com.creditcardmanager.viewmodel.ActivityViewModel
import com.creditcardmanager.viewmodel.BankViewModel
import com.creditcardmanager.viewmodel.CardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ActivityDetailFragment : Fragment() {
    private var _binding: FragmentCardDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by viewModels()
    private val bankViewModel: BankViewModel by viewModels()
    private val cardViewModel: CardViewModel by viewModels()
    private val args: ActivityDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectActivity(args.activityId)
        observeData()
        // 长按 root 弹操作菜单（编辑/调整进度/归档/删除）
        binding.root.setOnLongClickListener {
            viewModel.selectedActivity.value?.activity?.let { showActionDialog(it) }
            true
        }
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
        val progress = detail.progress

        // 你 fragment_card_detail.xml 里的 ID 全都有，安全调用 ?. 兜底
        binding.tvCardName?.text = activity.name
        binding.tvBankName?.text = detail.bankName ?: detail.cardName ?: "通用活动"
        binding.tvCreditLimit?.text = "类型: ${activity.type.toDisplayName()}"
        binding.tvStatementDay?.text = "周期: ${activity.periodType.toDisplayName()}"
        binding.tvDueDate?.text = "目标: ${getTargetText(activity)}"
        binding.tvInterestFree?.text = "进度: ${getProgressText(detail)}"
        binding.tvStatementAmount?.text = "¥${String.format("%.2f", progress.currentAmount)}"

        // 手动调整提示（用 tvAnnualFee 这行，原本是年费规则，活动页没用，复用）
        binding.tvAnnualFee?.text = buildString {
            if (activity.isArchived) append("[已归档] ")
            if (progress.manualBaseline != null) {
                val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                val t = sdf.format(Date(progress.manualSince ?: 0))
                append("手动调整于$t 基准¥${String.format("%.2f", progress.manualBaseline)}")
            } else {
                append("长按编辑/调整进度/删除")
            }
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

    private fun com.creditcardmanager.model.enums.PeriodType.toDisplayName(): String = when (this) {
        com.creditcardmanager.model.enums.PeriodType.NATURAL_DAY -> "自然日"
        com.creditcardmanager.model.enums.PeriodType.NATURAL_WEEK -> "自然周"
        com.creditcardmanager.model.enums.PeriodType.NATURAL_MONTH -> "自然月"
        com.creditcardmanager.model.enums.PeriodType.NATURAL_QUARTER -> "自然季度"
        com.creditcardmanager.model.enums.PeriodType.BIND_STATEMENT -> "绑定账单周期"
        com.creditcardmanager.model.enums.PeriodType.ONE_TIME -> "一次性"
    }

    private fun getTargetText(activity: Activity): String = when (activity.type) {
        ActivityType.AMOUNT_TARGET -> "消费满 ¥${activity.targetAmount ?: 0}"
        ActivityType.COUNT_TARGET -> "消费 ${activity.targetCount ?: 0} 笔"
        ActivityType.CASHBACK_RATE -> "返现比例 ${(activity.cashbackRate ?: 0.0) * 100}%"
        ActivityType.CONTINUOUS_PERIOD -> "连续 ${activity.requiredPeriods ?: 1} 期达标"
        ActivityType.FIRST_SPEND -> "首笔消费满 ¥${activity.minAmount ?: 0}"
        ActivityType.CHECKIN_DAILY -> "每日签到"
        ActivityType.CONSECUTIVE_DAYS -> "连续消费 ${activity.targetCount ?: 0} 天"
        ActivityType.WEEKLY_CLAIM -> "每周固定日期领取"
    }

    private fun getProgressText(detail: ActivityViewModel.ActivityDetail): String {
        val p = detail.progress
        val activity = detail.activity
        return when (activity.type) {
            ActivityType.AMOUNT_TARGET -> "¥${String.format("%.2f", p.currentAmount)} / ¥${String.format("%.2f", activity.targetAmount ?: 0.0)}"
            ActivityType.COUNT_TARGET -> "${p.currentCount} / ${activity.targetCount ?: 0} 笔"
            ActivityType.CASHBACK_RATE -> "已返 ¥${String.format("%.2f", p.currentCashback)}"
            ActivityType.CONTINUOUS_PERIOD -> "连续 ${p.continuousDone} / ${activity.requiredPeriods ?: 1} 期"
            ActivityType.FIRST_SPEND -> if (p.isAchieved) "已完成" else "未完成"
            ActivityType.CHECKIN_DAILY -> if (p.isAchieved) "今日已签" else "今日未签"
            ActivityType.CONSECUTIVE_DAYS -> "已连续 ${p.currentCount} / ${activity.targetCount ?: 0} 天"
            ActivityType.WEEKLY_CLAIM -> if (p.isAchieved) "本周已领取" else "本周未领取"
        }
    }

    /** 调整进度弹窗 */
    private fun showAdjustProgressDialog() {
        val detail = viewModel.selectedActivity.value ?: return
        val activity = detail.activity
        val progress = detail.progress
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manual_adjust_progress, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.et_amount)
        val etCount = dialogView.findViewById<EditText>(R.id.et_count)
        val etCashback = dialogView.findViewById<EditText>(R.id.et_cashback)
        val tvCurrent = dialogView.findViewById<TextView>(R.id.tv_current_progress)

        // 按活动类型显隐输入框
        when (activity.type) {
            ActivityType.AMOUNT_TARGET, ActivityType.CASHBACK_RATE -> {
                etCount.visibility = View.GONE
                etCashback.visibility = if (activity.type == ActivityType.CASHBACK_RATE) View.VISIBLE else View.GONE
            }
            ActivityType.COUNT_TARGET -> {
                etAmount.visibility = View.GONE
                etCashback.visibility = View.GONE
            }
            else -> {
                etCount.visibility = View.GONE
                etCashback.visibility = View.GONE
            }
        }

        // 当前 final 值（manualBaseline + 调整后以来新消费，这里垫 all 消费的 manualBaseline+txnSum 近似）
        val currentFinal = (progress.manualBaseline ?: 0.0) + when (activity.type) {
            ActivityType.AMOUNT_TARGET -> detail.transactions.sumOf { it.amount }
            ActivityType.COUNT_TARGET -> detail.transactions.size.toDouble()
            ActivityType.CASHBACK_RATE -> progress.currentCashback
            else -> 0.0
        }
        tvCurrent.text = "当前进度：${getProgressText(detail)}"
        when (activity.type) {
            ActivityType.AMOUNT_TARGET -> etAmount.setText(currentFinal.toString())
            ActivityType.COUNT_TARGET -> etCount.setText(currentFinal.toInt().toString())
            ActivityType.CASHBACK_RATE -> etCashback.setText(currentFinal.toString())
            else -> etAmount.setText(currentFinal.toString())
        }

AlertDialog.Builder(requireContext())
            .setTitle("调整进度")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val baseline = when (activity.type) {
                    ActivityType.AMOUNT_TARGET -> etAmount.text.toString().toDoubleOrNull() ?: currentFinal
                    ActivityType.COUNT_TARGET -> etCount.text.toString().toDoubleOrNull() ?: currentFinal
                    ActivityType.CASHBACK_RATE -> etCashback.text.toString().toDoubleOrNull() ?: currentFinal
                    else -> etAmount.text.toString().toDoubleOrNull() ?: currentFinal
                }
                viewModel.manualAdjustProgress(
                    activityId = activity.id,
                    periodKey = progress.periodKey,
                    baseline = baseline,
                    source = "CHECK"
                )
                Toast.makeText(requireContext(), "进度已调整", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("以当前消费为基准") { _, _ ->
                viewModel.manualAdjustProgress(
                    activityId = activity.id,
                    periodKey = progress.periodKey,
                    baseline = currentFinal,
                    source = "MANUAL"
                )
                Toast.makeText(requireContext(), "已设为当前消费基准，旧消费不再计入", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /** 长按 root 弹的操作菜单（编辑 / 调整进度 / 归档或恢复 / 删除） */
    private fun showActionDialog(activity: Activity) {
        val options = mutableListOf<String>()
        options.add("编辑活动")
        options.add("调整进度")          // ← 新增，长按就能进
        if (activity.isArchived) {
            options.add("恢复活动")
            options.add("彻底删除")
        } else {
            options.add("归档活动")
            options.add("删除活动")
        }
        AlertDialog.Builder(requireContext())
            .setTitle(activity.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "编辑活动" -> showEditDialog()
                    "调整进度" -> showAdjustProgressDialog()
                    "归档活动" -> showArchiveDialog()
                    "恢复活动" -> {
                        viewModel.unarchiveActivity(activity.id)
                        Toast.makeText(requireContext(), "已恢复", Toast.LENGTH_SHORT).show()
                    }
                    "删除活动", "彻底删除" -> showDeleteDialog()
                }
            }
            .show()
    }

    private fun showEditDialog() {
        val activity = viewModel.selectedActivity.value?.activity ?: return
        EditActivityDialog(activity, bankViewModel, cardViewModel, viewModel).show(childFragmentManager, "edit_activity")
    }

    private fun showArchiveDialog() {
        val activity = viewModel.selectedActivity.value?.activity ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("确认归档")
            .setMessage("确定归档「${activity.name}」？")
            .setPositiveButton("归档") { _, _ ->
                viewModel.archiveActivity(activity.id)
                Toast.makeText(requireContext(), "已归档", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteDialog() {
        val activity = viewModel.selectedActivity.value?.activity ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除后将无法恢复，确定删除「${activity.name}」？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteActivity(activity)
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}