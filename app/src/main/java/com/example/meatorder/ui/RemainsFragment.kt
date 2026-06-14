package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.data.entity.MeatEntity
import com.example.meatorder.databinding.FragmentRemainsBinding
import com.example.meatorder.utils.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RemainsFragment : Fragment() {
    private var _binding: FragmentRemainsBinding? = null
    private val binding get() = _binding!!

    private var flatList = listOf<Any>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemainsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(getPrefs().headerColor)
        header?.title = "Остатки"
        applyFontSize(binding.root, getPrefs().fontSize)

        val dao = getDao()
        lifecycleScope.launch {
            val inputTypes = dao.getAllInputTypes().first()
            dao.getAllEntities().collect { entities ->
                setupList(entities, inputTypes)
            }
        }
    }

    private fun setupList(entities: List<MeatEntity>, inputTypes: List<InputType>) {
        val grouped = entities.groupBy { it.group }
        val newFlatList = mutableListOf<Any>()
        val sortedGroups = grouped.keys.sorted()
        for (group in sortedGroups) {
            newFlatList.add(group)
            val sortedEntities = grouped[group]!!.sortedBy { it.entity }
            newFlatList.addAll(sortedEntities)
        }
        flatList = newFlatList

        val adapter = RemainsAdapter(
            fragment = this@RemainsFragment,
            items = flatList,
            inputTypes = inputTypes,
            onDataChanged = {},
            highlight = true,
            remainData = mutableMapOf()
        )

        binding.recyclerRemains.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRemains.addItemDecoration(
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.bottom = 1
                }
            }
        )
        binding.recyclerRemains.addItemDecoration(
            StickyHeaderItemDecoration(
                getItems = { flatList },
                headerHeight = 120,
                backgroundColor = 0xFFF0F0F0.toInt(),
                textColor = 0xFF333333.toInt(),
                getTextSize = { getPrefs().fontSize.toFloat() },
                textSizeOffset = 20f
            )
        )
        binding.recyclerRemains.adapter = adapter

        binding.fabContinue.setOnClickListener {
            lifecycleScope.launch {
                val remainData = adapter.remainData
                val minOrderItems = getDao().getAllMinOrderItems().first()
                val allInputTypes = getDao().getAllInputTypes().first()  // свежий список

                val orderList = mutableListOf<Map<String, Any>>()
                for (minItem in minOrderItems) {
                    val entity = entities.find { it.id == minItem.entity_id } ?: continue

                    // Тип единиц минимального заказа
                    val minType = allInputTypes.find { it.type_name == minItem.input_type } ?: continue
                    val minWeight = minType.weight_kg  // вес одной единицы минимального типа

                    // Находим остаток по этому entity_id
                    val remainPair = remainData[minItem.entity_id]
                    if (remainPair != null && remainPair.second > 0 && remainPair.first != null) {
                        val remainType = remainPair.first!!
                        val remainQty = remainPair.second

                        // Если типы совпадают – считаем как раньше
                        if (remainType.type_name == minItem.input_type) {
                            val diff = minItem.quantity - remainQty
                            if (diff > 0) {
                                orderList.add(mapOf(
                                    "entity_id" to minItem.entity_id,
                                    "entity" to entity.entity,
                                    "group" to entity.group,
                                    "input_type" to minItem.input_type,
                                    "quantity" to diff
                                ))
                            }
                        } else {
                            // Конвертация через вес
                            val remainWeightKg = remainQty * remainType.weight_kg
                            val minWeightKg = minItem.quantity * minWeight

                            if (minWeightKg > remainWeightKg) {
                                val diffKg = minWeightKg - remainWeightKg
                                // Переводим разницу обратно в единицы минимального типа
                                val diff = kotlin.math.ceil(diffKg / minWeight).toInt()
                                if (diff > 0) {
                                    orderList.add(mapOf(
                                        "entity_id" to minItem.entity_id,
                                        "entity" to entity.entity,
                                        "group" to entity.group,
                                        "input_type" to minItem.input_type,
                                        "quantity" to diff
                                    ))
                                }
                            }
                        }
                    } else {
                        // Остаток не указан – включаем весь минимальный заказ
                        orderList.add(mapOf(
                            "entity_id" to minItem.entity_id,
                            "entity" to entity.entity,
                            "group" to entity.group,
                            "input_type" to minItem.input_type,
                            "quantity" to minItem.quantity
                        ))
                    }
                }

                val selectedJson = com.google.gson.Gson().toJson(orderList)
                val bundle = Bundle().apply {
                    putBoolean("byBalance", true)
                    putIntArray("templateIds", intArrayOf())
                    putString("initialItemsJson", selectedJson)
                }
                findNavController().navigate(R.id.action_remainsFragment_to_order2Fragment, bundle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
