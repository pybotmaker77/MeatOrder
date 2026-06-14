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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.data.entity.TemplateItem
import com.example.meatorder.databinding.FragmentOrder2Binding
import com.example.meatorder.utils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Order2Fragment : Fragment() {
    private var _binding: FragmentOrder2Binding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: Order2Adapter
    private var allItems = mutableListOf<Order2Item>()
    private var inputTypes = listOf<InputType>()

    private val flatListForHeader: List<Any>
        get() = allItems.map { it.entity ?: it.group ?: "" }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrder2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(getPrefs().headerColor)
        applyFontSize(binding.root, getPrefs().fontSize)

        val dao = getDao()
        val args = arguments
        val byBalance = args?.getBoolean("byBalance", false) ?: false
        val templateIds = args?.getIntArray("templateIds")?.toList() ?: emptyList()
        val initialItemsJson = args?.getString("initialItemsJson")

        lifecycleScope.launch {
            inputTypes = dao.getAllInputTypes().first()
            val entities = dao.getAllEntities().first()
            val templateItems = mutableListOf<TemplateItem>()
            for (tId in templateIds) {
                val items = dao.getTemplateItems(tId).first()
                templateItems.addAll(items)
            }

            // Предзаполнение из initialItemsJson (для остатков)
            val initialMap = mutableMapOf<Int, Pair<String, Int>>() // entityId -> (input_type, quantity)
            if (!initialItemsJson.isNullOrEmpty()) {
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                val list: List<Map<String, Any>> = Gson().fromJson(initialItemsJson, type)
                for (item in list) {
                    val entityId = (item["entity_id"] as Double).toInt()
                    val inputType = item["input_type"] as String
                    val quantity = (item["quantity"] as Double).toInt()
                    initialMap[entityId] = Pair(inputType, quantity)
                }
            }

            val grouped = entities.groupBy { it.group }
            val list = mutableListOf<Order2Item>()
            for ((group, ents) in grouped) {
                list.add(Order2Item(entity = null, group = group))
                for (ent in ents) {
                    val templateItem = templateItems.find { it.entity_id == ent.id }
                    val initial = initialMap[ent.id]
                    val selected = if (initial != null) true
                        else if (byBalance) false
                        else templateItem != null
                    val item = Order2Item(
                        entity = ent,
                        group = group,
                        selected = selected,
                        inputType = if (initial != null) inputTypes.find { it.type_name == initial.first } else null,
                        quantity = initial?.second ?: (templateItem?.input_default ?: 0)
                    )
                    list.add(item)
                }
            }
            allItems = list
            adapter = Order2Adapter(this@Order2Fragment, { item, position ->
                showSelectFormDialog(item, position)
            }, inputTypes)
            binding.recyclerOrder2.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerOrder2.addItemDecoration(
                StickyHeaderItemDecoration(
                    getItems = { flatListForHeader },
                    headerHeight = 120,
                    backgroundColor = 0xFFF0F0F0.toInt(),
                    textColor = 0xFF333333.toInt(),
                    getTextSize = { getPrefs().fontSize.toFloat() },
                    textSizeOffset = 20f
                )
            )
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
                    val bundle = Bundle().apply {
                        putString("selectedItemsJson", selectedJson)
                    }
                    findNavController().navigate(R.id.action_order2Fragment_to_order3Fragment, bundle)
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

        val dialog = AlertDialog.Builder(requireContext())
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
            .setNegativeButton("Отмена") { _, _ ->
                item.selected = false
                item.inputType = null
                item.quantity = 0
                adapter.notifyItemChanged(position)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
