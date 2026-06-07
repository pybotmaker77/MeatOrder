package com.example.meatorder.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.dao.AppDao
import com.example.meatorder.data.entity.*
import com.example.meatorder.databinding.FragmentDirectoryEditBinding
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DirectoryEditFragment : Fragment() {
    private var _binding: FragmentDirectoryEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecyclerView.Adapter<*>
    private var dict: String = "entities"

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

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.fabAdd.setOnClickListener { showAddDialog(dict) }

        val dao = getDao()
        lifecycleScope.launch {
            when (dict) {
                "entities" -> setupEntities(dao)
                "templates" -> setupTemplates(dao)
                "input_types" -> setupInputTypes(dao)
                "patterns" -> setupPatterns(dao)
            }
        }
    }

    private suspend fun setupEntities(dao: AppDao) {
        dao.getAllEntities().collectLatest { entities ->
            adapter = object : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_2, parent, false)
                    return ViewHolder(itemView)
                }
                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val entity = entities[position]
                    holder.text1.text = entity.entity
                    holder.text2?.text = "Группа: ${entity.group}"
                    holder.itemView.setOnLongClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Удалить")
                            .setMessage("Удалить \"${entity.entity}\"?")
                            .setPositiveButton("Да") { _, _ ->
                                lifecycleScope.launch {
                                    dao.deleteEntity(entity)
                                }
                            }
                            .setNegativeButton("Нет", null)
                            .show()
                        true
                    }
                }
                override fun getItemCount() = entities.size
            }
            binding.recyclerView.adapter = adapter
        }
    }

    private suspend fun setupTemplates(dao: AppDao) {
        dao.getAllTemplates().collectLatest { templates ->
            adapter = object : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_1, parent, false)
                    return ViewHolder(itemView)
                }
                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val template = templates[position]
                    holder.text1.text = template.temp
                    holder.itemView.setOnClickListener {
                        val bundle = Bundle().apply {
                            putInt("templateId", template.id)
                            putString("templateName", template.temp)
                        }
                        findNavController().navigate(R.id.action_directoryEditFragment_to_templateEditFragment, bundle)
                    }
                }
                override fun getItemCount() = templates.size
            }
            binding.recyclerView.adapter = adapter
        }
    }

    private suspend fun setupInputTypes(dao: AppDao) {
        dao.getAllInputTypes().collectLatest { inputTypes ->
            adapter = object : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_2, parent, false)
                    return ViewHolder(itemView)
                }
                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val type = inputTypes[position]
                    holder.text1.text = type.type_name
                    holder.text2?.text = "Сокр.: ${type.short_name}, Вес: ${type.weight_kg} кг"
                }
                override fun getItemCount() = inputTypes.size
            }
            binding.recyclerView.adapter = adapter
        }
    }

    private suspend fun setupPatterns(dao: AppDao) {
        dao.getAllPatterns().collectLatest { patterns ->
            adapter = object : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_1, parent, false)
                    return ViewHolder(itemView)
                }
                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val pattern = patterns[position]
                    holder.text1.text = pattern.name + if (pattern.is_active) " (активен)" else ""
                    holder.itemView.setOnClickListener {
                        showEditPatternDialog(pattern)
                    }
                    holder.itemView.setOnLongClickListener {
                        if (pattern.id == 1) {
                            Toast.makeText(requireContext(), "Базовый паттерн нельзя удалить", Toast.LENGTH_SHORT).show()
                        } else {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Удалить паттерн")
                                .setMessage("Удалить \"${pattern.name}\"?")
                                .setPositiveButton("Да") { _, _ ->
                                    lifecycleScope.launch {
                                        dao.deletePattern(pattern)
                                    }
                                }
                                .setNegativeButton("Нет", null)
                                .show()
                        }
                        true
                    }
                }
                override fun getItemCount() = patterns.size
            }
            binding.recyclerView.adapter = adapter
        }
    }

    private fun showAddDialog(dict: String) {
        when (dict) {
            "entities" -> {
                val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
                val etName = EditText(requireContext()).apply { hint = "Наименование" }
                val etGroup = EditText(requireContext()).apply { hint = "Группа" }
                layout.addView(etName)
                layout.addView(etGroup)
                AlertDialog.Builder(requireContext())
                    .setTitle("Добавить позицию")
                    .setView(layout)
                    .setPositiveButton("Добавить") { _, _ ->
                        val name = etName.text.toString().trim()
                        val group = etGroup.text.toString().trim().ifEmpty { "Без группы" }
                        if (name.isNotEmpty()) {
                            lifecycleScope.launch {
                                getDao().insertEntity(MeatEntity(entity = name, group = group))
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            "templates" -> {
                val etName = EditText(requireContext()).apply { hint = "Название шаблона" }
                AlertDialog.Builder(requireContext())
                    .setTitle("Создать шаблон")
                    .setView(etName)
                    .setPositiveButton("Создать") { _, _ ->
                        val name = etName.text.toString().trim()
                        if (name.isNotEmpty()) {
                            lifecycleScope.launch {
                                getDao().insertTemplate(Template(temp = name))
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            "input_types" -> {
                val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
                val etName = EditText(requireContext()).apply { hint = "Название (например, Коробка)" }
                val etShort = EditText(requireContext()).apply { hint = "Сокращение (например, кор.)" }
                val etWeight = EditText(requireContext()).apply { hint = "Вес, кг (например, 10)" }
                layout.addView(etName)
                layout.addView(etShort)
                layout.addView(etWeight)
                AlertDialog.Builder(requireContext())
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
            }
            "patterns" -> {
                val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
                val etName = EditText(requireContext()).apply { hint = "Название паттерна" }
                val etTemplate = EditText(requireContext()).apply { hint = "Текст паттерна (например, - {entity} - {input} {input_type_short}.)" }
                layout.addView(etName)
                layout.addView(etTemplate)
                AlertDialog.Builder(requireContext())
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
            }
        }
    }

    private fun showEditPatternDialog(pattern: Pattern) {
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val etName = EditText(requireContext()).apply { setText(pattern.name) }
        val etTemplate = EditText(requireContext()).apply { setText(pattern.template) }
        layout.addView(etName)
        layout.addView(etTemplate)

        AlertDialog.Builder(requireContext())
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
