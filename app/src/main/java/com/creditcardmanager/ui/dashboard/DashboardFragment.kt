package com.creditcardmanager.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.creditcardmanager.R
import com.creditcardmanager.databinding.FragmentDashboardBinding
import com.creditcardmanager.ui.activities.ActivityAdapter
import com.creditcardmanager.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val paymentAdapter by lazy { PaymentAdapter() }
    private val activityAdapter by lazy { ActivityAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPayments.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPayments.adapter = paymentAdapter
        binding.rvActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivities.adapter = activityAdapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDashboard() }
        binding.btnAddTransaction.setOnClickListener { findNavController().navigate(R.id.action_dashboard_to_addTransaction) }
        binding.btnViewAllCards.setOnClickListener { findNavController().navigate(R.id.action_dashboard_to_cards) }
        binding.btnViewAllActivities.setOnClickListener { findNavController().navigate(R.id.action_dashboard_to_activities) }
        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isLoading.collect { binding.swipeRefresh.isRefreshing = it } }
                launch { viewModel.dashboardData.collect { data -> updateUI(data) } }
            }
        }
    }

    private fun updateUI(data: com.creditcardmanager.model.DashboardData) {
        binding.topCardsContainer.removeAllViews()
        data.topCards.forEach { info ->
            val cardView = layoutInflater.inflate(R.layout.item_top_card, binding.topCardsContainer, false)
            cardView.findViewById<android.widget.TextView>(R.id.tv_card_name).text = "${info.bankShortName ?: ""} ${info.cardName}"
            cardView.findViewById<android.widget.TextView>(R.id.tv_due_date).text = "还款日: ${info.dueDate}"
            cardView.findViewById<android.widget.TextView>(R.id.tv_days).text = info.interestFreeDays.toString()
            binding.topCardsContainer.addView(cardView)
        }
        paymentAdapter.submitList(data.upcomingPayments)
        val allActivities = data.bankActivities + data.cardActivities
        activityAdapter.submitList(allActivities)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
