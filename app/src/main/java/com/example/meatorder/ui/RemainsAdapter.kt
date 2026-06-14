package com.example.meatorder.ui

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.data.entity.MeatEntity
import com.example.meatorder.databinding.ItemRemainsBinding
import com.example.meatorder.utils.applyFontSize
import com.example.meatorder.utils.getPrefs

class RemainsAdapter(
    private val fragment: androidx.fragment.app.Fragment,
    private val items: List<Any>,               // String (заголовок) или MeatEntity
    private val inputTypes: List<InputType>,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // Храним для каждой позиции выбранный тип и количество
    private val remainData = mutableMapOf<Int, Pair<InputType?, Int>>() // entityId -> (тип, количество)

    fun getRemainData(): Map<Int, Pair<InputType?, Int>> = remainData

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = TextView(parent.context).apply {
                    setPadding(32, 16, 16, 8)
                    setBackgroundColor(0xFFF0F0F0.toInt())
                    setTextColor(0xFF333333.toInt())
                }
                object : RecyclerView.ViewHolder(view) {}
            }
            else -> {
                val binding = ItemRemainsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                applyFontSize(binding.root, fragment.getPrefs().fontSize)
                EntityViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(item as String)
            is EntityViewHolder -> holder.bind(item as MeatEntity)
        }
    }

    override fun getItemCount() = items.size

    // Для повторного использования старого ViewHolder
    inner class EntityViewHolder(private val binding: ItemRemainsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(entity: MeatEntity) {
            binding.tvEntity.text = entity.entity
            val data = remainData[entity.id]
            if (data != null && data.second > 0 && data.first != null) {
                binding.tvRemainSummary.text = "${data.second} ${data.first!!.short_name}"
                binding.tvRemainSummary.visibility = View.VISIBLE
            } else {
                binding.tvRemainSummary.visibility = View.GONE
            }

            binding.btnAddRemain.setOnClickListener {
                showRemainDialog(entity)
            }
        }

        private fun showRemainDialog(entity: MeatEntity) {
            val dialogView = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_select_form, null)
            applyFontSize(dialogView, fragment.getPrefs().fontSize)

            val rgTypes = dialogView.findViewById<RadioGroup>(R.id.rgTypes)
            val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)

            for (type in inputTypes) {
                val rb = RadioButton(fragment.requireContext())
                rb.text = type.type_name
                rgTypes.addView(rb)
            }

            val currentData = remainData[entity.id]
            if (currentData != null && currentData.first != null) {
                val index = inputTypes.indexOfFirst { it.type_name == currentData.first!!.type_name }
                if (index >= 0) {
                    (rgTypes.getChildAt(index) as? RadioButton)?.isChecked = true
                }
            }
            etQuantity.setText(currentData?.second?.toString() ?: "")

            val dialog = AlertDialog.Builder(fragment.requireContext())
                .setTitle("Выберите форму")
                .setView(dialogView)
                .setPositiveButton("Выбрать") { _, _ ->
                    val selectedId = rgTypes.checkedRadioButtonId
                    if (selectedId != -1) {
                        val selectedIndex = rgTypes.indexOfChild(dialogView.findViewById(selectedId))
                        val selectedType = inputTypes[selectedIndex]
                        val qty = etQuantity.text.toString().toIntOrNull() ?: 0
                        remainData[entity.id] = Pair(selectedType, qty)
                        notifyItemChanged(adapterPosition)
                        onDataChanged()
                    }
                }
                .setNegativeButton("Отмена", null)
                .create()

            dialog.setOnShowListener { dialogInterface ->
                (dialogInterface as? AlertDialog)?.let {
                    it.window?.decorView?.let { rootView ->
                        applyFontSize(rootView, fragment.getPrefs().fontSize)
                    }
                    it.getButton(AlertDialog.BUTTON_POSITIVE)?.let { btn ->
                        applyFontSize(btn, fragment.getPrefs().fontSize)
                    }
                    it.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { btn ->
                        applyFontSize(btn, fragment.getPrefs().fontSize)
                    }
                }
            }
            dialog.show()
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(title: String) {
            (itemView as TextView).text = title
            applyFontSize(itemView, fragment.getPrefs().fontSize, fragment.getPrefs().fontSize + 2)
        }
    }
}
