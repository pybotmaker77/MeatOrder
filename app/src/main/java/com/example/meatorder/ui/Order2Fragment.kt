package com.example.meatorder.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meatorder.R
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.data.entity.TemplateItem
import com.example.meatorder.databinding.FragmentOrder2Binding
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Order2Fragment : Fragment() {
    private var _binding: FragmentOrder2Binding? = null
    private val binding get() = _binding!!
    private val args: Order2FragmentArgs by navArgs()
    private lateinit var adapter: Order2Adapter
    private var allItems = mutableListOf<Order2Item>()
    private var inputTypes = listOf<InputType>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrder2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.setNavigationOnClickListener { findNavController().popBackStack() }

        val dao = getDao()
        lifecycleScope.launch {
            inputTypes = dao.getAllInputTypes().first()
            val entities = dao.getAllEntities().first()
            val templateItems = mutableListOf<TemplateItem>()
            if (args.templateIds.isNotEmpty()) {
                for (tId in args.templateIds) {
                    val items = dao.getTemplateItems(tId).first()
                    templateItems.addAll(items)
                }
            }
            val grouped = entities.groupBy { it.group }
            val list = mutableListOf<Order2Item>()
            for ((group, ents) in grouped) {
                list.add(Order2Item(entity = null, group = group))
                for (ent in ents) {
                    val templateItem = templateItems.find { it.entity_id == ent.id }
                    val selected = if (args.byBalance && ent.id in args.preSelectedIds.toSet()) true
                    else templateItem != null
                    val item = Order2Item(
                        entity = ent,
                        group = group,
                        selected = selected,
                        inputType = if (templateItem != null) inputTypes.find { it.type_name == templateItem.input_type } else null,
                        quantity = templateItem?.input_default ?: 0
                    )
                    list.add(item)
                }
            }
            allItems = list
            adapter = Order2Adapter({ item, position ->
                showSelectFormDialog(item, position)
            }, inputTypes)
            binding.recyclerOrder2.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerOrder2.adapter = adapter
            adapter.submitList(allItems)

            binding.fabSubmit.setOnClickListener {
                val selected = allItems.filter {
                    it.entity != null && it.selected && it.inputType != null && it.quantity > 0
                }
                if (selected.isEmpty()) {
                    showToast("Выберите хотя бы одну позицию")
                } else {
                    val selectedJson = Gson().toJson(selected.map {
                        mapOf(
                            "entity_id" to it.entity!!.id,
                            "entity" to it.entity.entity,
                            "group" to it.group,
                            "input_type" to it.inputType!!.type_name,
                            "quantity" to it.quantity
                        )
                    })
                    getPrefs().saveDraft(selectedJson)
                    val action = Order2FragmentDirections.actionOrder2FragmentToOrder3Fragment(
                        selectedItemsJson = selectedJson
                    )
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun showSelectFormDialog(item: Order2Item, position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_form, null)
        val rgTypes = dialogView.findViewById<RadioGroup>(R.id.rgTypes)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)

        for (type in inputTypes) {
            val rb = RadioButton(requireContext())
            rb.text = type.type_name
            rgTypes.addView(rb)
        }
        if (item.inputType != null) {
            val index = inputTypes.indexOfFirst { it.type_name == item.inputType!!.type_name }
            if (index >= 0) {
                (rgTypes.getChildAt(index) as? RadioButton)?.isChecked = true
            }
        }
        etQuantity.setText(item.quantity.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Выберите форму")
            .setView(dialogView)
            .setPositiveButton("Выбрать") { _, _ ->
                val selectedId = rgTypes.checkedRadioButtonId
                if (selectedId != -1) {
                    val selectedIndex = rgTypes.indexOfChild(dialogView.findViewById(selectedId))
                    val selectedType = inputTypes[selectedIndex]
                    val qty = etQuantity.text.toString().toIntOrNull() ?: 0
                    item.inputType = selectedType
                    item.quantity = qty
                    adapter.notifyItemChanged(position)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
