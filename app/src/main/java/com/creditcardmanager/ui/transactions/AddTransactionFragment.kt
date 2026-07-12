package com.creditcardmanager.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.creditcardmanager.databinding.FragmentAddTransactionBinding
import com.creditcardmanager.viewmodel.TransactionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class AddTransactionFragment : Fragment() {
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editDate.setText(LocalDate.now().toString())
        observeData()
        setupListeners()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cards.collect { cards ->
                        binding.spinnerCard.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cards.map { it.getDisplayName() })
                    }
                }
                launch {
                    viewModel.tags.collect { tags ->
                        binding.spinnerTag.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tags.map { it.name })
                    }
                }
                launch {
                    viewModel.preview.collect { preview ->
                        preview?.let { updatePreview(it) }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnPreview.setOnClickListener { previewTransaction() }
        binding.btnSave.setOnClickListener { saveTransaction() }
    }

    private fun previewTransaction() {
        val cardPos = binding.spinnerCard.selectedItemPosition
        val amount = binding.editAmount.text.toString().toDoubleOrNull() ?: return
        val dateStr = binding.editDate.text.toString()
        val tagPos = binding.spinnerTag.selectedItemPosition
        viewModel.previewTransaction(
            cardId = viewModel.cards.value.getOrNull(cardPos)?.id ?: return,
            amount = amount, spendDate = LocalDate.parse(dateStr),
            tagId = viewModel.tags.value.getOrNull(tagPos)?.id ?: return
        )
    }

    private fun saveTransaction() {
        val cardPos = binding.spinnerCard.selectedItemPosition
        val amount = binding.editAmount.text.toString().toDoubleOrNull() ?: return
        val dateStr = binding.editDate.text.toString()
        val tagPos = binding.spinnerTag.selectedItemPosition
        val channel = binding.editChannel.text.toString().takeIf { it.isNotBlank() }
        val note = binding.editNote.text.toString().takeIf { it.isNotBlank() }
        viewModel.createTransaction(
            cardId = viewModel.cards.value.getOrNull(cardPos)?.id ?: return,
            amount = amount, spendDate = LocalDate.parse(dateStr),
            tagId = viewModel.tags.value.getOrNull(tagPos)?.id ?: return,
            channel = channel, note = note
        )
        Toast.makeText(requireContext(), "消费录入成功", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    private fun updatePreview(preview: TransactionViewModel.TransactionPreview) {
        binding.previewContainer.visibility = View.VISIBLE
        binding.tvInterestFreeDays.text = "免息期: ${preview.interestFreeDays}天"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
