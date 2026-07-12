package com.creditcardmanager.ui.transactions

import android.app.DatePickerDialog
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
import java.util.Calendar

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
        binding.editDate.setOnClickListener { showDatePicker() }
        binding.btnPreview.setOnClickListener { previewTransaction() }
        binding.btnSave.setOnClickListener { saveTransaction() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        if (binding.editDate.text?.isNotBlank() == true) {
            try {
                val d = LocalDate.parse(binding.editDate.text)
                cal.set(d.year, d.monthValue - 1, d.dayOfMonth)
            } catch (_: Exception) { }
        }
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                binding.editDate.setText("%04d-%02d-%02d".format(y, m + 1, d))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
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
        binding.tvInterestFreeDays.text = "免息期: ${preview.interestFreeDays} 天"

        val activityNames = preview.matchedActivities.map { it.activity.name }
        binding.tvPreviewActivities.text = if (activityNames.isNotEmpty()) 
            "命中活动: ${activityNames.joinToString(", ")}" 
        else "暂无命中活动"

        val totalRebate = preview.matchedActivities.mapNotNull { it.cashbackPreview }.sum()
        binding.tvPreviewRebate.text = if (totalRebate > 0) 
            "预计返现: ¥${String.format("%.2f", totalRebate)}" 
        else ""
        binding.tvPreviewRebate.visibility = if (totalRebate > 0) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}