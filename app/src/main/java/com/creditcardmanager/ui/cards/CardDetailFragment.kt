package com.creditcardmanager.ui.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.creditcardmanager.databinding.FragmentCardDetailBinding
import com.creditcardmanager.viewmodel.CardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CardDetailFragment : Fragment() {
    private var _binding: FragmentCardDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CardViewModel by viewModels()
    private val args: CardDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectCard(args.cardId)
        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCard.collect { detail -> detail?.let { updateUI(it) } }
            }
        }
    }

    private fun updateUI(detail: CardViewModel.CardDetail) {
        binding.tvCardName.text = detail.card.getDisplayName()
        binding.tvBankName.text = detail.bank?.name ?: ""
        binding.tvCreditLimit.text = "额度: ${detail.card.creditLimit?.let { "¥$it" } ?: "--"}"
        binding.tvStatementDay.text = "账单日: 每月${detail.card.statementDay}日"
        binding.tvDueDate.text = "还款日: ${detail.interestFreeInfo.dueDate}"
        binding.tvInterestFree.text = "当前免息期: ${detail.interestFreeInfo.interestFreeDays}天"
        binding.tvStatementAmount.text = "本期账单: ¥${detail.statementAmount}"
        detail.annualFeeProgress?.let {
            binding.tvAnnualFee.text = "年费进度: ${it.currentAmount}元 / ${it.currentCount}笔 ${if (it.isAchieved) "✓" else ""}"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
