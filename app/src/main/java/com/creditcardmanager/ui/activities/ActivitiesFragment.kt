package com.creditcardmanager.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.R
import com.creditcardmanager.databinding.FragmentActivitiesBinding
import com.creditcardmanager.ui.dialog.AddActivityDialog
import com.creditcardmanager.viewmodel.ActivityViewModel
import com.creditcardmanager.viewmodel.BankViewModel
import com.creditcardmanager.viewmodel.CardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ActivitiesFragment : Fragment() {
    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by viewModels()
    private val bankViewModel: BankViewModel by viewModels()
    private val cardViewModel: CardViewModel by viewModels()
    private val adapter by lazy {
        ActivityAdapter(
            onItemClick = { activity ->
                val action = ActivitiesFragmentDirections.actionActivitiesToActivityDetail(activity.activity.id)
                findNavController().navigate(action)
            },
            onItemLongClick = { activityWithProgress ->
                showActivityActions(activityWithProgress)
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadActivities(ActivityViewModel.ActivityFilter.ALL)
                    1 -> viewModel.loadActivities(ActivityViewModel.ActivityFilter.BANK)
                    2 -> viewModel.loadActivities(ActivityViewModel.ActivityFilter.CARD)
                    3 -> viewModel.loadActivities(ActivityViewModel.ActivityFilter.GENERAL)
                    4 -> viewModel.loadActivities(ActivityViewModel.ActivityFilter.ARCHIVED)
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        binding.btnSort.setOnClickListener { showSortMenu(it) }

        binding.fabAddActivity.setOnClickListener {
            AddActivityDialog(bankViewModel, cardViewModel, viewModel)
                .show(childFragmentManager, "add_activity")
        }

        setupSwipeToDelete()
        observeData()
    }

    private fun showSortMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_activity_sort, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sort_by_bank -> viewModel.setSortType(ActivityViewModel.SortType.BY_BANK)
                    R.id.sort_by_card -> viewModel.setSortType(ActivityViewModel.SortType.BY_CARD)
                    R.id.sort_by_type -> viewModel.setSortType(ActivityViewModel.SortType.BY_TYPE)
                    R.id.sort_by_progress -> viewModel.setSortType(ActivityViewModel.SortType.BY_PROGRESS)
                }
                true
            }
            show()
        }
    }

    private fun showActivityActions(item: com.creditcardmanager.model.ActivityWithProgress) {
        val activity = item.activity
        val options = if (activity.isArchived) {
            arrayOf("恢复活动", "彻底删除")
        } else {
            arrayOf("编辑活动", "归档活动", "删除活动")
        }
        AlertDialog.Builder(requireContext())
            .setTitle(activity.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "编辑活动" -> {
                        val action = ActivitiesFragmentDirections.actionActivitiesToActivityDetail(activity.id)
                        findNavController().navigate(action)
                    }
                    "归档活动" -> viewModel.archiveActivity(activity.id)
                    "恢复活动" -> viewModel.unarchiveActivity(activity.id)
                    "删除活动", "彻底删除" -> confirmDelete(activity)
                }
            }
            .show()
    }

    private fun confirmDelete(activity: com.creditcardmanager.model.Activity) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除后将无法恢复，确定删除「${activity.name}」？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteActivity(activity)
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
                val item = adapter.getItemAt(position)
                if (item.activity.isArchived) {
                    confirmDelete(item.activity)
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle(item.activity.name)
                        .setItems(arrayOf("归档", "删除")) { _, which ->
                            when (which) {
                                0 -> viewModel.archiveActivity(item.activity.id)
                                1 -> confirmDelete(item.activity)
                            }
                        }
                        .setOnDismissListener { adapter.notifyItemChanged(position) }
                        .show()
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerView)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activities.collect { list ->
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
