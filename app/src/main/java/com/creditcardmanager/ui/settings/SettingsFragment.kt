package com.creditcardmanager.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.creditcardmanager.databinding.FragmentSettingsBinding
import com.creditcardmanager.utils.AppSettings
import com.creditcardmanager.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var appSettings: AppSettings

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val json = requireContext().contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
            viewModel.importData(json)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnExport.setOnClickListener { viewModel.exportData() }
        binding.btnImport.setOnClickListener { importLauncher.launch("application/json") }
        binding.btnClearData.setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle("确认清除").setMessage("这将删除所有数据，不可恢复！")
                .setPositiveButton("确定") { _, _ -> viewModel.clearAllData(); Toast.makeText(requireContext(), "数据已清除", Toast.LENGTH_SHORT).show() }
                .setNegativeButton("取消", null).show()
        }

        // Payment reminder settings
        binding.btnPaymentSettings.setOnClickListener { showPaymentSettingsDialog() }

        // Activity reminder settings
        binding.btnActivityReminderSettings.setOnClickListener { showActivityReminderSettingsDialog() }

        observeData()
    }

    private fun showPaymentSettingsDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val editDaysAhead = EditText(requireContext()).apply {
            setText(appSettings.paymentDaysAhead.toString())
            hint = "首页显示几天内到期的还款"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val spinnerReminderTimes = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
                listOf("当天提醒", "提前1天+当天", "提前3天+1天+当天", "提前7天+3天+1天+当天"))
            setSelection(appSettings.paymentReminderTimes - 1)
        }

        val editHour = EditText(requireContext()).apply {
            setText(appSettings.paymentReminderHour.toString())
            hint = "提醒小时 (0-23)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val editMinute = EditText(requireContext()).apply {
            setText(appSettings.paymentReminderMinute.toString())
            hint = "提醒分钟 (0-59)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(editDaysAhead)
        layout.addView(spinnerReminderTimes)
        layout.addView(editHour)
        layout.addView(editMinute)

        AlertDialog.Builder(requireContext())
            .setTitle("还款提醒设置")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val days = editDaysAhead.text.toString().toIntOrNull() ?: 7
                val times = spinnerReminderTimes.selectedItemPosition + 1
                val hour = editHour.text.toString().toIntOrNull() ?: 10
                val minute = editMinute.text.toString().toIntOrNull() ?: 0

                appSettings.paymentDaysAhead = days.coerceIn(1, 30)
                appSettings.paymentReminderTimes = times.coerceIn(1, 4)
                appSettings.paymentReminderHour = hour.coerceIn(0, 23)
                appSettings.paymentReminderMinute = minute.coerceIn(0, 59)

                Toast.makeText(requireContext(), "设置已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showActivityReminderSettingsDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val editHour = EditText(requireContext()).apply {
            setText(appSettings.activityReminderHour.toString())
            hint = "提醒小时 (0-23)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(editHour)

        AlertDialog.Builder(requireContext())
            .setTitle("活动提醒设置")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val hour = editHour.text.toString().toIntOrNull() ?: 10
                appSettings.activityReminderHour = hour.coerceIn(0, 23)
                Toast.makeText(requireContext(), "设置已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.exportResult.collect { path ->
                        Toast.makeText(requireContext(), "已导出到: $path", Toast.LENGTH_LONG).show()
                    }
                }
                launch {
                    viewModel.importResult.collect { success ->
                        Toast.makeText(requireContext(), if (success) "导入成功" else "导入失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
