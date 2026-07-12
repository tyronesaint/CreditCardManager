package com.creditcardmanager.ui.reminders

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.creditcardmanager.databinding.FragmentRemindersBinding
import com.creditcardmanager.model.Reminder
import com.creditcardmanager.model.ReminderTime
import com.creditcardmanager.model.enums.SourceType
import com.creditcardmanager.utils.IdGenerator
import com.creditcardmanager.viewmodel.ReminderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RemindersFragment : Fragment() {
    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReminderViewModel by viewModels()
    private val adapter by lazy {
        ReminderAdapter(
            onToggle = { reminder, enabled ->
                viewModel.setEnabled(reminder.id, enabled)
            },
            onComplete = { reminder ->
                viewModel.setCompleted(reminder.id, true)
                Toast.makeText(requireContext(), "已标记完成", Toast.LENGTH_SHORT).show()
            },
            onDelete = { reminder ->
                confirmDelete(reminder)
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.fabAddReminder.setOnClickListener {
            AddReminderDialog { reminder ->
                viewModel.addReminder(reminder)
            }.show(childFragmentManager, "add_reminder")
        }
        observeData()
    }

    private fun confirmDelete(reminder: Reminder) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除提醒")
            .setMessage("确定删除「${reminder.title}」？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteReminder(reminder)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reminders.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
