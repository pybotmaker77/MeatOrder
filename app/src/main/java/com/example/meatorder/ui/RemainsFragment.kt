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
                val remainData = adapter.getRemainData()
                val minOrderItems = getDao().getAllMinOrderItems().first()

                // Рассчитываем заказ
                val orderList = mutableListOf<Map<String, Any>>()
                for ((entityId, pair) in remainData) {
                    val (remainType, remainQty) = pair
                    if (remainType == null || remainQty == 0) continue
                    val minItem = minOrderItems.find { it.entity_id == entityId && it.input_type == remainType.type_name }
                    if (minItem != null) {
                        val diff = minItem.quantity - remainQty
                        if (diff > 0) {
                            orderList.add(mapOf(
                                "entity_id" to entityId,
                                "entity" to (entities.find { it.id == entityId }?.entity ?: ""),
                                "group" to (entities.find { it.id == entityId }?.group ?: ""),
                                "input_type" to remainType.type_name,
                                "quantity" to diff
                            ))
                        }
                    }
                }

                // Если ничего не добавилось, передаём пустой список
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
