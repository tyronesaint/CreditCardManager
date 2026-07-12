package com.creditcardmanager.ui.reminders

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.databinding.FragmentRemindersBinding
import com.creditcardmanager.model.Reminder
import com.creditcardmanager.model.ReminderRepeatType
import com.creditcardmanager.model.ReminderTime
import com.creditcardmanager.viewmodel.ReminderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RemindersFragment : Fragment() {
    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReminderViewModel by viewModels()

    private val adapter: ReminderAdapter by lazy {
        ReminderAdapter(
            onToggle = { reminder: Reminder, enabled: Boolean ->
                val action = if (enabled) "启用" else "禁用"
                AlertDialog.Builder(requireContext())
                    .setTitle("确认${action}")
                    .setMessage("确定${action}提醒「${reminder.title}」？")
                    .setPositiveButton("确定") { _, _ ->
                        viewModel.setEnabled(reminder.id, enabled)
                        Toast.makeText(requireContext(), "已${action}", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消") { _, _ ->
                        adapter.notifyDataSetChanged()
                    }
                    .setOnCancelListener {
                        adapter.notifyDataSetChanged()
                    }
                    .show()
            },
            onComplete = { reminder: Reminder, completed: Boolean ->
                if (completed) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("标记完成")
                        .setMessage("确定将「${reminder.title}」标记为已完成？")
                        .setPositiveButton("完成") { _, _ ->
                            viewModel.setCompleted(reminder.id, true)
                            Toast.makeText(requireContext(), "已标记完成", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("取消") { _, _ ->
                            adapter.notifyDataSetChanged()
                        }
                        .setOnCancelListener {
                            adapter.notifyDataSetChanged()
                        }
                        .show()
                } else {
                    viewModel.setCompleted(reminder.id, false)
                }
            },
            onLongClick = { reminder: Reminder ->
                showReminderActions(reminder)
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
            AddReminderDialog { reminder: Reminder ->
                viewModel.addReminder(reminder)
                Toast.makeText(requireContext(), "提醒已添加", Toast.LENGTH_SHORT).show()
            }.show(childFragmentManager, "add_reminder")
        }
        setupSwipeToDelete()
        observeData()
    }

    private fun showReminderActions(reminder: Reminder) {
        AlertDialog.Builder(requireContext())
            .setTitle(reminder.title)
            .setItems(arrayOf("编辑", "删除")) { _, which ->
                when (which) {
                    0 -> showEditReminderDialog(reminder)
                    1 -> confirmDelete(reminder)
                }
            }
            .show()
    }

    private fun showEditReminderDialog(reminder: Reminder) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val editTitle = EditText(requireContext()).apply {
            setText(reminder.title)
            hint = "提醒标题"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val spinnerRepeat = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
                listOf("一次性", "每天", "每周", "每月几号", "每月几次"))
            setSelection(reminder.repeatType.ordinal)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editRepeatValue = EditText(requireContext()).apply {
            setText(reminder.repeatValue ?: "")
            hint = "具体值（周几1-7 / 几号1-31 / 几次）"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editOffset = EditText(requireContext()).apply {
            setText(reminder.remindTimes.firstOrNull()?.offsetDays?.toString() ?: "0")
            hint = "提前几天提醒"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editTime = EditText(requireContext()).apply {
            setText(reminder.remindTimes.firstOrNull()?.timeOfDay ?: "09:00")
            hint = "提醒时间（如 09:00）"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        layout.addView(editTitle)
        layout.addView(spinnerRepeat)
        layout.addView(editRepeatValue)
        layout.addView(editOffset)
        layout.addView(editTime)

        // 根据重复类型显示/隐藏具体值输入
        spinnerRepeat.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                when (position) {
                    0, 1 -> editRepeatValue.visibility = View.GONE
                    2 -> {
                        editRepeatValue.visibility = View.VISIBLE
                        editRepeatValue.hint = "周几（1=周一, 7=周日）"
                    }
                    3 -> {
                        editRepeatValue.visibility = View.VISIBLE
                        editRepeatValue.hint = "每月几号（1-31）"
                    }
                    4 -> {
                        editRepeatValue.visibility = View.VISIBLE
                        editRepeatValue.hint = "每月几次"
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        // Trigger initial visibility
        spinnerRepeat.onItemSelectedListener?.onItemSelected(null, null, reminder.repeatType.ordinal, 0)

        AlertDialog.Builder(requireContext())
            .setTitle("编辑提醒")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val repeatType = ReminderRepeatType.values()[spinnerRepeat.selectedItemPosition]
                val repeatValue = if (repeatType == ReminderRepeatType.WEEKLY ||
                                      repeatType == ReminderRepeatType.MONTHLY_DATE ||
                                      repeatType == ReminderRepeatType.MONTHLY_COUNT) {
                    editRepeatValue.text.toString().trim().takeIf { it.isNotEmpty() }
                } else null

                // 验证
                if (repeatType == ReminderRepeatType.WEEKLY) {
                    val v = repeatValue?.toIntOrNull()
                    if (v == null || v !in 1..7) {
                        Toast.makeText(requireContext(), "周几必须是1-7", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }
                if (repeatType == ReminderRepeatType.MONTHLY_DATE) {
                    val v = repeatValue?.toIntOrNull()
                    if (v == null || v !in 1..31) {
                        Toast.makeText(requireContext(), "日期必须是1-31", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }
                if (repeatType == ReminderRepeatType.MONTHLY_COUNT) {
                    val v = repeatValue?.toIntOrNull()
                    if (v == null || v < 1) {
                        Toast.makeText(requireContext(), "次数必须大于0", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }

                val offset = editOffset.text.toString().toIntOrNull() ?: 0
                val time = editTime.text.toString().takeIf { it.isNotBlank() } ?: "09:00"

                val updated = reminder.copy(
                    title = title,
                    remindTimes = listOf(ReminderTime(offsetDays = offset, timeOfDay = time)),
                    repeatType = repeatType,
                    repeatValue = repeatValue,
                    completed = false
                )
                viewModel.updateReminder(updated)
                Toast.makeText(requireContext(), "已更新", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDelete(reminder: Reminder) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除提醒")
            .setMessage("确定删除「${reminder.title}」？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteReminder(reminder)
                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.adapterPosition
                val reminder = adapter.getItemAt(position)
                AlertDialog.Builder(requireContext())
                    .setTitle("删除提醒")
                    .setMessage("确定删除「${reminder.title}」？")
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteReminder(reminder)
                    }
                    .setNegativeButton("取消") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerView)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reminders.collect { list: List<Reminder> ->
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
