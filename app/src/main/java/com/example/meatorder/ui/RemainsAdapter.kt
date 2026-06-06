package com.example.meatorder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.data.entity.MeatEntity
import com.example.meatorder.databinding.ItemRemainsBinding

class RemainsAdapter(
    private val items: List<MeatEntity>,
    private val onQuantityChanged: (Int, Int) -> Unit
) : RecyclerView.Adapter<RemainsAdapter.ViewHolder>() {

    private val quantities = mutableMapOf<Int, Int>()

    fun getQuantities(): Map<Int, Int> = quantities

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRemainsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entity = items[position]
        holder.bind(entity, quantities[entity.id])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemRemainsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(entity: MeatEntity, currentQty: Int?) {
            binding.tvEntity.text = entity.entity
            binding.etQuantity.setText(currentQty?.toString() ?: "")
            binding.etQuantity.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val qty = binding.etQuantity.text.toString().toIntOrNull() ?: 0
                    quantities[entity.id] = qty
                    onQuantityChanged(entity.id, qty)
                }
            }
        }
    }
}
