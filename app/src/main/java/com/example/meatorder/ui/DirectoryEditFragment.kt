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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.*
import com.example.meatorder.databinding.FragmentDirectoryEditBinding
import com.example.meatorder.utils.getDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DirectoryEditFragment : Fragment() {
    private var _binding: FragmentDirectoryEditBinding? = null
    private val binding get() = _binding!!
    private val args: DirectoryEditFragmentArgs by navArgs()
    private lateinit var adapter: RecyclerView.Adapter<*>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectoryEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.fabAdd.setOnClickListener { showAddDialog(args.dict) }

        val dao = getDao()
        lifecycleScope.launch {
            when (args.dict) {
                "entities" -> setupEntities(dao)
                "templates" -> setupTemplates(dao)
                "input_types" -> setupInputTypes(dao)
                "patterns" -> setupPatterns(dao)
            }
        }
    }

    private suspend fun setupEntities(dao: com.example.meatorder.data.dao.AppDao) {
        val entities = dao.getAllEntities().first()
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
                                // Обновить список можно, пересоздав фрагмент или повторно подписавшись.
                                // Для простоты перезапустим фрагмент.
                                findNavController().navigateUp()
                                findNavController().navigate(R.id.directoryEditFragment)
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

    private suspend fun setupTemplates(dao: com.example.meatorder.data.dao.AppDao) {
        val templates = dao.getAllTemplates().first()
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
                    Toast.makeText(requireContext(), "Редактирование шаблона (в разработке)", Toast.LENGTH_SHORT).show()
                }
            }
            override fun getItemCount() = templates.size
        }
        binding.recyclerView.adapter = adapter
    }

    private suspend fun setupInputTypes(dao: com.example.meatorder.data.dao.AppDao) {
        val inputTypes = dao.getAllInputTypes().first()
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

    private suspend fun setupPatterns(dao: com.example.meatorder.data.dao.AppDao) {
        val patterns = dao.getAllPatterns().first()
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
                    lifecycleScope.launch {
                        dao.deactivateAllPatterns()
                        dao.activatePattern(pattern.id)
                        Toast.makeText(requireContext(), "Паттерн \"${pattern.name}\" активирован", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun getItemCount() = patterns.size
        }
        binding.recyclerView.adapter = adapter
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
                                // Перезагрузим фрагмент, чтобы увидеть изменения
                                findNavController().navigateUp()
                                findNavController().navigate(R.id.directoryEditFragment)
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
                                findNavController().navigateUp()
                                findNavController().navigate(R.id.directoryEditFragment)
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
                                findNavController().navigateUp()
                                findNavController().navigate(R.id.directoryEditFragment)
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            "patterns" -> {
                Toast.makeText(requireContext(), "Добавление паттернов пока недоступно", Toast.LENGTH_SHORT).show()
            }
        }
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
