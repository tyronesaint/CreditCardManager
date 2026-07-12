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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.creditcardmanager.databinding.FragmentPaymentRemindersBinding
import com.creditcardmanager.viewmodel.PaymentRemindersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentRemindersFragment : Fragment() {
    private var _binding: FragmentPaymentRemindersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentRemindersViewModel by viewModels()
    private val adapter by lazy {
        PaymentReminderAdapter(
            onMarkPaid = { payment ->
                AlertDialog.Builder(requireContext())
                    .setTitle("确认还款")
                    .setMessage("确定「${payment.cardName}」已还款？")
                    .setPositiveButton("已还款") { _, _ ->
                        viewModel.markAsPaid(payment.cardId)
                        Toast.makeText(requireContext(), "已标记还款", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            },
            onItemClick = { payment ->
                val action = PaymentRemindersFragmentDirections.actionPaymentRemindersToCardDetail(payment.cardId)
                findNavController().navigate(action)
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadPayments() }
        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.payments.collect { list ->
                        adapter.submitList(list)
                        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.isLoading.collect { binding.swipeRefresh.isRefreshing = it }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
