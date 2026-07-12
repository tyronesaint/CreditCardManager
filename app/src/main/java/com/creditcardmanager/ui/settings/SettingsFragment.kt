package com.creditcardmanager.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.creditcardmanager.databinding.FragmentSettingsBinding
import com.creditcardmanager.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

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
        observeData()
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
