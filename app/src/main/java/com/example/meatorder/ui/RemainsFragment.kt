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
import com.example.meatorder.utils.applyFontSize
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RemainsFragment : Fragment() {
    private var _binding: FragmentRemainsBinding? = null
    private val binding get() = _binding!!

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

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
        // Группировка и сортировка
        val grouped = entities.groupBy { it.group }
        val flatList = mutableListOf<Any>()
        // Сортируем группы по алфавиту
        val sortedGroups = grouped.keys.sorted()
        for (group in sortedGroups) {
            flatList.add(group)
            val sortedEntities = grouped[group]!!.sortedBy { it.entity }
            flatList.addAll(sortedEntities)
        }

        val adapter = RemainsAdapter(this@RemainsFragment, flatList.filterIsInstance<MeatEntity>(), inputTypes) {
            // Данные изменились, ничего не делаем
        }

        // Свой адаптер для группировки
        val groupingAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            override fun getItemViewType(position: Int): Int {
                return if (flatList[position] is String) TYPE_HEADER else TYPE_ITEM
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return when (viewType) {
                    TYPE_HEADER -> {
                        val view = android.widget.TextView(parent.context).apply {
                            setPadding(32, 16, 16, 8)
                            setBackgroundColor(0xFFF0F0F0.toInt())
                            setTextColor(0xFF333333.toInt())
                        }
                        object : RecyclerView.ViewHolder(view) {}
                    }
                    else -> {
                        adapter.onCreateViewHolder(parent, viewType)
                    }
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = flatList[position]
                when (holder.itemViewType) {
                    TYPE_HEADER -> {
                        (holder.itemView as android.widget.TextView).text = item as String
                        applyFontSize(holder.itemView, getPrefs().fontSize, getPrefs().fontSize + 2)
                    }
                    TYPE_ITEM -> {
                        val entity = item as MeatEntity
                        val entityIndex = adapter.items.indexOf(entity)
                        if (entityIndex != -1) {
                            adapter.onBindViewHolder(holder as RemainsAdapter.ViewHolder, entityIndex)
                        }
                    }
                }
            }

            override fun getItemCount() = flatList.size
        }

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
        binding.recyclerRemains.adapter = groupingAdapter

        binding.fabContinue.setOnClickListener {
            val remainData = adapter.getRemainData()
            // Передаём только те позиции, для которых не указан остаток (quantity == 0)
            val emptyEntities = entities.filter { entity ->
                val data = remainData[entity.id]
                data == null || data.second == 0
            }.map { it.id }.toIntArray()

            val bundle = Bundle().apply {
                putBoolean("byBalance", true)
                putIntArray("templateIds", intArrayOf())
                putIntArray("preSelectedIds", emptyEntities)
            }
            findNavController().navigate(R.id.action_remainsFragment_to_order2Fragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
