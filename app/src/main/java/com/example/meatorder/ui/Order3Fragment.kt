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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.databinding.FragmentOrder3Binding
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import com.example.meatorder.utils.showToast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Order3Fragment : Fragment() {
    private var _binding: FragmentOrder3Binding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: Order3Adapter
    private var items = mutableListOf<Order3Item>()
    private var inputTypes = listOf<InputType>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrder3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedJson = arguments?.getString("selectedItemsJson") ?: return
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val rawList: List<Map<String, Any>> = Gson().fromJson(selectedJson, type)

        lifecycleScope.launch {
            inputTypes = getDao().getAllInputTypes().first()
            items.clear()
            for (raw in rawList) {
                val inputType = inputTypes.find { it.type_name == raw["input_type"] }
                inputType?.let {
                    items.add(
                        Order3Item(
                            entityId = (raw["entity_id"] as Double).toInt(),
                            entity = raw["entity"] as String,
                            group = raw["group"] as String,
                            inputType = it,
                            quantity = (raw["quantity"] as Double).toInt()
                        )
                    )
                }
            }
            items.sortWith(compareBy<Order3Item> { it.group }
                .thenByDescending { it.quantity }
                .thenBy { it.entity })

            adapter = Order3Adapter(
                onEditClick = { item -> showEditDialog(item) },
                onDeleteClick = { item -> deleteItem(item) }
            )
            binding.recyclerOrder3.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerOrder3.adapter = adapter
            adapter.submitList(items.toList())

            val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ) = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        deleteItem(items[position])
                    }
                }
            }
            ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerOrder3)

            updateSummary()

            binding.fabContinue.setOnClickListener {
                if (items.isEmpty()) {
                    showToast("Список пуст")
                } else {
                    val json = Gson().toJson(items.map {
                        mapOf(
                            "entity_id" to it.entityId,
                            "entity" to it.entity,
                            "group" to it.group,
                            "input_type" to it.inputType.type_name,
                            "quantity" to it.quantity
                        )
                    })
                    val bundle = Bundle().apply {
                        putString("finalItemsJson", json)
                    }
                    findNavController().navigate(R.id.action_order3Fragment_to_order4Fragment, bundle)
                }
            }
        }
    }

    private fun updateSummary() {
        val blocks = items.filter { it.inputType.type_name == "Блок" }.sumOf { it.quantity }
        val bags = items.filter { it.inputType.type_name == "Мешок" }.sumOf { it.quantity }
        val kg = items.filter { it.inputType.type_name == "Кг" }.sumOf { it.quantity }
        val totalWeight = items.sumOf { it.quantity * it.inputType.weight_kg }
        binding.tvSummary.text =
            "Итого: Блоков: $blocks | Мешков: $bags | Кг: $kg\nОбщий вес: $totalWeight кг"
    }

    private fun deleteItem(item: Order3Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить?")
            .setMessage("Удалить позицию \"${item.entity}\"?")
            .setPositiveButton("Да") { _, _ ->
                items.remove(item)
                adapter.submitList(items.toList())
                updateSummary()
                saveDraft()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun showEditDialog(item: Order3Item) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_form, null)
        val rg = dialogView.findViewById<RadioGroup>(R.id.rgTypes)
        val etQty = dialogView.findViewById<EditText>(R.id.etQuantity)

        for (type in inputTypes) {
            val rb = RadioButton(requireContext())
            rb.text = type.type_name
            rg.addView(rb)
        }
        val index = inputTypes.indexOfFirst { it.type_name == item.inputType.type_name }
        if (index >= 0) (rg.getChildAt(index) as RadioButton).isChecked = true
        etQty.setText(item.quantity.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Редактировать")
            .setView(dialogView)
            .setPositiveButton("Выбрать") { _, _ ->
                val selectedId = rg.checkedRadioButtonId
                if (selectedId != -1) {
                    val selIndex = rg.indexOfChild(dialogView.findViewById(selectedId))
                    val newType = inputTypes[selIndex]
                    val newQty = etQty.text.toString().toIntOrNull() ?: 0
                    val pos = items.indexOfFirst { it.entityId == item.entityId }
                    if (pos >= 0) {
                        items[pos] = items[pos].copy(inputType = newType, quantity = newQty)
                        adapter.notifyItemChanged(pos)
                        updateSummary()
                        saveDraft()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveDraft() {
        val json = Gson().toJson(items.map {
            mapOf(
                "entity_id" to it.entityId,
                "entity" to it.entity,
                "group" to it.group,
                "input_type" to it.inputType.type_name,
                "quantity" to it.quantity
            )
        })
        getPrefs().saveDraft(json)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
