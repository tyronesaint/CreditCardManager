package com.creditcardmanager.ui.activities

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.creditcardmanager.R
import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.ActivityProgress
import com.creditcardmanager.model.enums.ActivityType

/**
 * 手动调整进度弹窗（单独Dialog类，和现有AddActivityDialog风格统一）
 */
class ManualAdjustProgressDialog(
    private val activity: Activity,
    private val progress: ActivityProgress,
    private val onSave: (baseline: Double, source: String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manual_adjust_progress, null)
        val etAmount = view.findViewById<EditText>(R.id.et_amount)
        val etCount = view.findViewById<EditText>(R.id.et_count)
        val etCashback = view.findViewById<EditText>(R.id.et_cashback)
        val tvCurrent = view.findViewById<TextView>(R.id.tv_current_progress)

        // 根据活动类型显隐输入框
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

        // 显示当前进度
        tvCurrent.text = "当前进度：${getCurrentProgressText()}"
        // 垫当前final值
        val currentFinal = (progress.manualBaseline ?: 0.0) + getCurrentAccumulated()
        when (activity.type) {
            ActivityType.AMOUNT_TARGET -> etAmount.setText(currentFinal.toString())
            ActivityType.COUNT_TARGET -> etCount.setText(currentFinal.toInt().toString())
            ActivityType.CASHBACK_RATE -> etCashback.setText(currentFinal.toString())
            else -> etAmount.setText(currentFinal.toString())
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("调整进度")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val baseline = when (activity.type) {
                    ActivityType.AMOUNT_TARGET -> etAmount.text.toString().toDoubleOrNull() ?: currentFinal
                    ActivityType.COUNT_TARGET -> etCount.text.toString().toDoubleOrNull() ?: currentFinal
                    ActivityType.CASHBACK_RATE -> etCashback.text.toString().toDoubleOrNull() ?: currentFinal
                    else -> etAmount.text.toString().toDoubleOrNull() ?: currentFinal
                }
                onSave(baseline, "CHECK")
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("以当前消费为基准") { _, _ ->
                onSave(currentFinal, "MANUAL")
            }
            .create()
    }

    private fun getCurrentProgressText(): String {
        return when (activity.type) {
            ActivityType.AMOUNT_TARGET -> "¥${String.format("%.2f", progress.currentAmount)}"
            ActivityType.COUNT_TARGET -> "${progress.currentCount} 笔"
            ActivityType.CASHBACK_RATE -> "¥${String.format("%.2f", progress.currentCashback)}"
            else -> "¥${String.format("%.2f", progress.currentAmount)}"
        }
    }

    private fun getCurrentAccumulated(): Double {
        // 这里可以传已录消费的总和，简化版直接返回0（实际使用时在ActivityDetailFragment里计算后传入）
        return 0.0
    }
}

⚠️ 注：上面ActivityDetailFragment里已经用AlertDialog实现了调整逻辑，这个Dialog类可以不用，如果你想和现有Dialog风格统一就留，否则删掉这个文件，只用ActivityDetailFragment里的实现就行。
