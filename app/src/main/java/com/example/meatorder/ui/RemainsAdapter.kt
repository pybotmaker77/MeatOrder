package com.example.meatorder.ui

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.data.entity.MeatEntity
import com.example.meatorder.databinding.ItemRemainsBinding
import com.example.meatorder.utils.applyFontSize
import com.example.meatorder.utils.getPrefs

class RemainsAdapter(
    private val fragment: androidx.fragment.app.Fragment,
    private val items: List<MeatEntity>,
    private val inputTypes: List<InputType>,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<RemainsAdapter.ViewHolder>() {

    // Храним для каждой позиции выбранный тип и количество
    private val remainData = mutableMapOf<Int, Pair<InputType?, Int>>() // entityId -> (тип, количество)

    fun getRemainData(): Map<Int, Pair<InputType?, Int>> = remainData

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRemainsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Применяем текущий размер шрифта к строке
        applyFontSize(binding.root, fragment.getPrefs().fontSize)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entity = items[position]
        holder.bind(entity)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemRemainsBinding) :
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
                .inflate(com.example.meatorder.R.layout.dialog_select_form, null)
            applyFontSize(dialogView, fragment.getPrefs().fontSize)

            val rgTypes = dialogView.findViewById<RadioGroup>(com.example.meatorder.R.id.rgTypes)
            val etQuantity = dialogView.findViewById<EditText>(com.example.meatorder.R.id.etQuantity)

            // Добавляем радиокнопки для каждого типа единиц
            for (type in inputTypes) {
                val rb = RadioButton(fragment.requireContext())
                rb.text = type.type_name
                rgTypes.addView(rb)
            }

            // Если ранее был выбран тип, восстанавливаем его
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
}
