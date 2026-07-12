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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.databinding.FragmentCardsBinding
import com.creditcardmanager.ui.dialog.AddBankDialog
import com.creditcardmanager.ui.dialog.AddCardDialog
import com.creditcardmanager.ui.dialog.EditCardDialog
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
        CardAdapter(
            onCardClick = { card ->
                val action = CardsFragmentDirections.actionCardsToCardDetail(card.id)
                findNavController().navigate(action)
            },
            onCardLongClick = { card ->
                showCardActions(card)
            }
        )
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
            lifecycleScope.launch {
                val hasBanks = bankViewModel.banks.value.isNotEmpty()
                if (hasBanks) {
                    AddCardDialog(bankViewModel, viewModel).show(childFragmentManager, "add_card")
                } else {
                    AddBankDialog(bankViewModel).show(childFragmentManager, "add_bank")
                }
            }
        }
        setupSwipeToDelete()
        observeData()
    }

    private fun showCardActions(card: com.creditcardmanager.model.Card) {
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
        EditCardDialog(card, bankViewModel) { updatedCard ->
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
                val card = adapter.getItemAt(position)
                AlertDialog.Builder(requireContext())
                    .setTitle("删除卡片")
                    .setMessage("确定删除「${card.getDisplayName()}」？")
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteCard(card)
                        Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
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
