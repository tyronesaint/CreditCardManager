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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.creditcardmanager.databinding.FragmentCardsBinding
import com.creditcardmanager.ui.dialog.AddBankDialog
import com.creditcardmanager.ui.dialog.AddCardDialog
import com.creditcardmanager.viewmodel.BankViewModel
import com.creditcardmanager.viewmodel.CardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CardsFragment : Fragment() {
    private var _binding: FragmentCardsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CardViewModel by viewModels()
    private val bankViewModel: BankViewModel by viewModels()
    private val adapter by lazy {
        CardAdapter { card ->
            val action = CardsFragmentDirections.actionCardsToCardDetail(card.id)
            findNavController().navigate(action)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.fabAddCard.setOnClickListener {
            // 如果没有银行，先提示添加银行
            lifecycleScope.launch {
                val hasBanks = bankViewModel.banks.value.isNotEmpty()
                if (hasBanks) {
                    AddCardDialog(bankViewModel, viewModel).show(childFragmentManager, "add_card")
                } else {
                    AddBankDialog(bankViewModel).show(childFragmentManager, "add_bank")
                }
            }
        }
        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cards.collect { data ->
                    val allCards = data.flatMap { it.second }
                    adapter.submitList(allCards)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
