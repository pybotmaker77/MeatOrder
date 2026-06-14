package com.example.meatorder.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.dao.AppDao
import com.example.meatorder.data.entity.*
import com.example.meatorder.databinding.FragmentDirectoryEditBinding
import com.example.meatorder.utils.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DirectoryEditFragment : Fragment() {
    private var _binding: FragmentDirectoryEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecyclerView.Adapter<*>
    private var dict: String = "entities"

    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleImportFile(it) }
    }

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    private var flatList = mutableListOf<Any>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectoryEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dict = arguments?.getString("dict") ?: "entities"

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(getPrefs().headerColor)

        val importButton = Button(requireContext()).apply {
            text = "Импорт"
            setOnClickListener { importFileLauncher.launch(arrayOf("*/*")) }
        }
        (binding.root as? LinearLayout)?.addView(importButton, 1)
        applyFontSize(importButton, getPrefs().fontSize)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.fabAdd.setOnClickListener { showAddDialog(dict) }

        if (dict != "min_order") {
            binding.recyclerView.addItemDecoration(
                StickyHeaderItemDecoration(
                    getItems = { flatList },
                    headerHeight = 120,
                    backgroundColor = 0xFFF0F0F0.toInt(),
                    textColor = 0xFF333333.toInt(),
                    getTextSize = { getPrefs().fontSize.toFloat() },
                    textSizeOffset = 20f
                )
            )
        }

        val dao = getDao()
        lifecycleScope.launch {
            when (dict) {
                "entities" -> setupEntities(dao)
                "templates" -> setupTemplates(dao)
                "input_types" -> setupInputTypes(dao)
                "patterns" -> setupPatterns(dao)
                "min_order" -> setupMinOrder(dao)
            }
        }
    }

    private fun handleImportFile(uri: android.net.Uri) { /* без изменений */ }

    private suspend fun setupEntities(dao: AppDao) { /* как в предыдущей версии */ }

    private suspend fun setupTemplates(dao: AppDao) { /* как в предыдущей версии */ }

    private suspend fun setupInputTypes(dao: AppDao) { /* как в предыдущей версии */ }

    private suspend fun setupPatterns(dao: AppDao) { /* как в предыдущей версии */ }

    private suspend fun setupMinOrder(dao: AppDao) {
        val entities = dao.getAllEntities().first()
        val inputTypes = dao.getAllInputTypes().first()

        dao.getAllMinOrderItems().collectLatest { minItems ->
            val grouped = entities.groupBy { it.group }
            val newFlatList = mutableListOf<Any>()
            val sortedGroups = grouped.keys.sorted()
            for (group in sortedGroups) {
                newFlatList.add(group)
                val sortedEntities = grouped[group]!!.sortedBy { it.entity }
                newFlatList.addAll(sortedEntities)
            }
            flatList = newFlatList

            val remainsAdapter = RemainsAdapter(
                fragment = this@DirectoryEditFragment,
                items = flatList,
                inputTypes = inputTypes,
                onDataChanged = { },
                highlight = false,
                remainData = mutableMapOf<Int, Pair<InputType?, Int>>().apply {
                    for (item in minItems) {
                        val type = inputTypes.find { it.type_name == item.input_type }
                        put(item.entity_id, Pair(type, item.quantity))
                    }
                },
                onSave = { entityId, type, qty ->
                    lifecycleScope.launch {
                        val existing = dao.getAllMinOrderItems().first().find { it.entity_id == entityId && it.input_type == type.type_name }
                        if (existing != null) {
                            dao.updateMinOrderItem(existing.copy(quantity = qty))
                        } else {
                            dao.insertMinOrderItem(MinOrderItem(entity_id = entityId, input_type = type.type_name, quantity = qty))
                        }
                    }
                },
                onDelete = { entityId ->
                    lifecycleScope.launch {
                        val items = dao.getAllMinOrderItems().first().filter { it.entity_id == entityId }
                        items.forEach { dao.deleteMinOrderItem(it) }
                    }
                }
            )

            binding.recyclerView.adapter = remainsAdapter
        }
    }

    // ========== Диалоги добавления ==========
    private fun showAddDialog(dict: String) {
        when (dict) {
            "entities" -> {
                val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
                val etName = EditText(requireContext()).apply { hint = "Наименование" }
                val etGroup = EditText(requireContext()).apply { hint = "Группа" }
                layout.addView(etName)
                layout.addView(etGroup)
                applyFontSize(layout, getPrefs().fontSize)
                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle("Добавить позицию")
                    .setView(layout)
                    .setPositiveButton("Добавить") { _, _ ->
                        val name = etName.text.toString().trim()
                        val group = etGroup.text.toString().trim().ifEmpty { "Без группы" }
                        if (name.isNotEmpty()) {
                            lifecycleScope.launch { getDao().insertEntity(MeatEntity(entity = name, group = group)) }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
            }
            "templates" -> {
                val etName = EditText(requireContext()).apply { hint = "Название шаблона" }
                applyFontSize(etName, getPrefs().fontSize)
                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle("Создать шаблон")
                    .setView(etName)
                    .setPositiveButton("Создать") { _, _ ->
                        val name = etName.text.toString().trim()
                        if (name.isNotEmpty()) {
                            lifecycleScope.launch { getDao().insertTemplate(Template(temp = name)) }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
            }
            "input_types" -> {
                val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
                val etName = EditText(requireContext()).apply { hint = "Название (например, Коробка)" }
                val etShort = EditText(requireContext()).apply { hint = "Сокращение (например, кор.)" }
                val etWeight = EditText(requireContext()).apply { hint = "Вес, кг (например, 10)" }
                layout.addView(etName)
                layout.addView(etShort)
                layout.addView(etWeight)
                applyFontSize(layout, getPrefs().fontSize)
                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle("Добавить единицу измерения")
                    .setView(layout)
                    .setPositiveButton("Добавить") { _, _ ->
                        val name = etName.text.toString().trim()
                        val short = etShort.text.toString().trim()
                        val weight = etWeight.text.toString().toDoubleOrNull() ?: 1.0
                        if (name.isNotEmpty() && short.isNotEmpty()) {
                            lifecycleScope.launch {
                                getDao().insertInputType(InputType(type_name = name, short_name = short, weight_kg = weight))
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
            }
            "patterns" -> {
                val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
                val etName = EditText(requireContext()).apply { hint = "Название паттерна" }
                val etTemplate = EditText(requireContext()).apply { hint = "Текст паттерна (например, - {entity} - {input} {input_type_short}.)" }
                layout.addView(etName)
                layout.addView(etTemplate)
                applyFontSize(layout, getPrefs().fontSize)
                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle("Добавить паттерн")
                    .setView(layout)
                    .setPositiveButton("Добавить") { _, _ ->
                        val name = etName.text.toString().trim()
                        val template = etTemplate.text.toString().trim()
                        if (name.isNotEmpty() && template.isNotEmpty()) {
                            lifecycleScope.launch {
                                getDao().insertPattern(Pattern(name = name, template = template, is_active = false))
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
            }
        }
    }

    private fun showEditPatternDialog(pattern: Pattern) {
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val etName = EditText(requireContext()).apply { setText(pattern.name) }
        val etTemplate = EditText(requireContext()).apply { setText(pattern.template) }
        layout.addView(etName)
        layout.addView(etTemplate)
        applyFontSize(layout, getPrefs().fontSize)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать паттерн")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = etName.text.toString().trim()
                val newTemplate = etTemplate.text.toString().trim()
                if (newName.isNotEmpty() && newTemplate.isNotEmpty()) {
                    lifecycleScope.launch {
                        val updated = Pattern(id = pattern.id, name = newName, template = newTemplate, is_active = pattern.is_active)
                        getDao().updatePattern(updated)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text1: TextView = itemView.findViewById(android.R.id.text1)
        val text2: TextView? = itemView.findViewById(android.R.id.text2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
