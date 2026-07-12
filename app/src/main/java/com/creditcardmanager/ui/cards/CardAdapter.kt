package com.creditcardmanager.ui.cards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.creditcardmanager.databinding.ItemCardBinding
import com.creditcardmanager.model.Card

class CardAdapter(
    private val onCardClick: (Card) -> Unit,
    private val onCardLongClick: ((Card) -> Unit)? = null
) : RecyclerView.Adapter<CardAdapter.ViewHolder>() {
    private var items: List<Card> = emptyList()

    fun submitList(list: List<Card>) {
        items = list
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): Card = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: Card) {
            binding.tvCardName.text = card.getDisplayName()
            binding.tvStatementDay.text = "账单日: ${card.statementDay}日"
            binding.tvCreditLimit.text = card.creditLimit?.let { "额度 ¥${String.format("%.0f", it)}" } ?: "额度 --"
            binding.root.setOnClickListener { onCardClick(card) }
            binding.root.setOnLongClickListener {
                onCardLongClick?.invoke(card)
                true
            }
        }
    }
}
