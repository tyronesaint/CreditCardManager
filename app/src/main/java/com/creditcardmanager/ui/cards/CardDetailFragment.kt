package com.creditcardmanager.ui.cards

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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.creditcardmanager.databinding.FragmentCardDetailBinding
import com.creditcardmanager.ui.activities.ActivityAdapter
import com.creditcardmanager.ui.dialog.EditCardDialog
import com.creditcardmanager.viewmodel.CardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CardDetailFragment : Fragment() {
    private var _binding: FragmentCardDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CardViewModel by viewModels()
    private val args: CardDetailFragmentArgs by navArgs()
    private val activityAdapter by lazy {
        ActivityAdapter(
            onItemClick = { activityWithProgress ->
                // 导航到活动详情
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectCard(args.cardId)
        binding.rvActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivities.adapter = activityAdapter
        observeData()
    }

    private fun showCardActions() {
        val card = viewModel.selectedCard.value?.card ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(card.getDisplayName())
            .setItems(arrayOf("编辑卡片", "删除卡片")) { _, which ->
                when (which) {
                    0 -> showEditCardDialog(card)
                    1 -> confirmDeleteCard(card)
                }
            }
            .show()
    }

    private fun showEditCardDialog(card: com.creditcardmanager.model.Card) {
        EditCardDialog(card) { updatedCard ->
            viewModel.updateCard(updatedCard)
            Toast.makeText(requireContext(), "卡片已更新", Toast.LENGTH_SHORT).show()
        }.show(childFragmentManager, "edit_card")
    }

    private fun confirmDeleteCard(card: com.creditcardmanager.model.Card) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除「${card.getDisplayName()}」将同时删除相关交易和活动，确定删除？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteCard(card)
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCard.collect { detail ->
                    detail?.let { updateUI(it) }
                }
            }
        }
    }

    private fun updateUI(detail: CardViewModel.CardDetail) {
        binding.tvCardName.text = detail.card.getDisplayName()
        binding.tvBankName.text = detail.bank?.name ?: ""
        binding.tvCreditLimit.text = "额度: ${detail.card.creditLimit?.let { "¥${String.format("%.0f", it)}" } ?: "--"}"
        binding.tvStatementDay.text = "账单日: 每月${detail.card.statementDay}日"
        binding.tvDueDate.text = "还款日: ${detail.interestFreeInfo.dueDate} (${detail.card.dueDayType.name})"
        binding.tvInterestFree.text = "当前免息期: ${detail.interestFreeInfo.interestFreeDays}天"
        binding.tvStatementAmount.text = "本期账单: ¥${String.format("%.2f", detail.statementAmount)}"
        detail.annualFeeProgress?.let {
            binding.tvAnnualFee.text = "年费进度: ${String.format("%.2f", it.currentAmount)}元 / ${it.currentCount}笔 ${if (it.isAchieved) "✓" else ""}"
        } ?: run {
            binding.tvAnnualFee.text = "年费进度: 未开启"
        }
        activityAdapter.submitList(detail.activities)

        binding.tvCardName.setOnLongClickListener {
            showCardActions()
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
