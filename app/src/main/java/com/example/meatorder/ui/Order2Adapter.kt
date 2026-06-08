package com.example.meatorder.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.databinding.ItemOrder2EntityBinding
import com.example.meatorder.databinding.ItemOrder2GroupBinding

class Order2Adapter(
    private val onEntityClick: (Order2Item, Int) -> Unit,
    private val inputTypes: List<InputType>
) : ListAdapter<Order2Item, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_GROUP = 0
        private const val VIEW_TYPE_ENTITY = 1
    }

    class DiffCallback : DiffUtil.ItemCallback<Order2Item>() {
        override fun areItemsTheSame(oldItem: Order2Item, newItem: Order2Item) =
            oldItem.entity?.id == newItem.entity?.id && oldItem.group == newItem.group
        override fun areContentsTheSame(oldItem: Order2Item, newItem: Order2Item) =
            oldItem == newItem
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).entity == null) VIEW_TYPE_GROUP else VIEW_TYPE_ENTITY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_GROUP -> {
                val b = ItemOrder2GroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                GroupViewHolder(b)
            }
            else -> {
                val b = ItemOrder2EntityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                EntityViewHolder(b)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is GroupViewHolder) holder.bind(item.group ?: "")
        else if (holder is EntityViewHolder) holder.bind(item)
    }

    inner class GroupViewHolder(private val binding: ItemOrder2GroupBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(group: String) { binding.tvGroup.text = group }
    }

    inner class EntityViewHolder(private val binding: ItemOrder2EntityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Order2Item) {
            binding.tvEntity.text = item.entity?.entity
            binding.checkbox.isChecked = item.selected

            if (item.selected && (item.inputType == null || item.quantity == 0)) {
                binding.root.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.yellow_highlight))
            } else {
                binding.root.setBackgroundColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
            }

            // Обработчик нажатия на всю строку
            binding.root.setOnClickListener {
                if (!item.selected) {
                    // Если не выбрано – отмечаем и открываем диалог
                    item.selected = true
                    binding.checkbox.isChecked = true
                    onEntityClick(item, adapterPosition)
                } else {
                    // Уже выбрано – просто открываем диалог (для редактирования)
                    onEntityClick(item, adapterPosition)
                }
            }

            // Показываем сводку, если заполнено
            if (item.inputType != null && item.quantity > 0) {
                binding.tvSummary.text = "${item.quantity} ${item.inputType!!.short_name}"
                binding.tvSummary.visibility = View.VISIBLE
            } else {
                binding.tvSummary.visibility = View.GONE
            }
        }
    }
}
