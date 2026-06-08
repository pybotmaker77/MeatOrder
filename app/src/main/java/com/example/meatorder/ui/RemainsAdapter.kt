package com.example.meatorder.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.data.entity.MeatEntity
import com.example.meatorder.databinding.ItemRemainsBinding
import com.example.meatorder.utils.applyFontSize
import com.example.meatorder.utils.getPrefs

class RemainsAdapter(
    private val fragment: androidx.fragment.app.Fragment,
    private val items: List<MeatEntity>,
    private val onQuantityChanged: (Int, Int) -> Unit
) : RecyclerView.Adapter<RemainsAdapter.ViewHolder>() {

    private val quantities = mutableMapOf<Int, Int>()

    fun getQuantities(): Map<Int, Int> = quantities

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRemainsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Применяем текущий размер шрифта к строке
        applyFontSize(binding.root, fragment.getPrefs().fontSize)
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
                if (hasFocus && binding.etQuantity.text.toString() == "0") {
                    binding.etQuantity.setText("")
                }
            }

            binding.etQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s.isNullOrEmpty()) return
                    if (s.toString() == "0" && s.length == 1 && binding.etQuantity.hasFocus()) {
                        binding.etQuantity.setText("")
                    }
                }
            })

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
