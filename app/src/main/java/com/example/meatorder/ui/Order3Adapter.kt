package com.example.meatorder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.databinding.ItemOrder3Binding

class Order3Adapter(
    private val onEditClick: (Order3Item) -> Unit,
    private val onDeleteClick: (Order3Item) -> Unit
) : ListAdapter<Order3Item, Order3Adapter.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<Order3Item>() {
        override fun areItemsTheSame(oldItem: Order3Item, newItem: Order3Item) =
            oldItem.entityId == newItem.entityId
        override fun areContentsTheSame(oldItem: Order3Item, newItem: Order3Item) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrder3Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemOrder3Binding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Order3Item) {
            binding.tvEntity.text = item.entity
            binding.tvQuantity.text = item.quantity.toString()
            binding.tvInputType.text = item.inputType.short_name
            binding.root.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
