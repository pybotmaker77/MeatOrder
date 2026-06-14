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

    private fun handleImportFile(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val text = readTextFromUri(uri, requireActivity())
                when (dict) {
                    "entities" -> {
                        val pairs = parseEntitiesCsv(text)
                        val entities = pairs.map { MeatEntity(entity = it.first, group = it.second) }
                        getDao().insertEntities(entities)
                        Toast.makeText(requireContext(), "Номенклатура импортирована (${entities.size} поз.)", Toast.LENGTH_SHORT).show()
                    }
                    "templates" -> {
                        val importTemplates = parseTemplatesJson(text)
                        for (importTemplate in importTemplates) {
                            val existing = getDao().getAllTemplates().first().find { it.temp == importTemplate.name }
                            if (existing != null) {
                                getDao().deleteTemplate(existing)
                            }
                            val newTemplate = Template(temp = importTemplate.name)
                            val newId = getDao().insertTemplate(newTemplate)
                            for (item in importTemplate.items) {
                                val entity = getDao().getAllEntities().first().find { it.entity == item.entity }
                                if (entity != null) {
                                    getDao().insertTemplateItem(
                                        TemplateItem(
                                            template_id = newId.toInt(),
                                            entity_id = entity.id,
                                            input_type = item.input_type,
                                            input_default = item.input_default
                                        )
                                    )
                                }
                            }
                        }
                        Toast.makeText(requireContext(), "Шаблоны импортированы (${importTemplates.size} шт.)", Toast.LENGTH_SHORT).show()
                    }
                    "input_types" -> {
                        Toast.makeText(requireContext(), "Импорт единиц измерения пока не поддерживается", Toast.LENGTH_SHORT).show()
                    }
                    "patterns" -> {
                        Toast.makeText(requireContext(), "Импорт паттернов пока не поддерживается", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ========== Номенклатура ==========
    private suspend fun setupEntities(dao: AppDao) {
        dao.getAllEntities().collectLatest { entities ->
            val grouped = entities.groupBy { it.group }
            val newFlatList = mutableListOf<Any>()
            for ((group, list) in grouped) {
                newFlatList.add(group)
                newFlatList.addAll(list)
            }
            flatList = newFlatList

            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                override fun getItemViewType(position: Int): Int {
                    return if (flatList[position] is String) TYPE_HEADER else TYPE_ITEM
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    return when (viewType) {
                        TYPE_HEADER -> {
                            val view = TextView(parent.context).apply {
                                setPadding(32, 16, 16, 8)
                                setBackgroundColor(0xFFF0F0F0.toInt())
                                setTextColor(0xFF333333.toInt())
                            }
                            object : RecyclerView.ViewHolder(view) {}
                        }
                        else -> {
                            val itemView = LayoutInflater.from(parent.context)
                                .inflate(R.layout.item_edit_button, parent, false)
                            object : RecyclerView.ViewHolder(itemView) {}
                        }
                    }
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = flatList[position]
                    when (holder.itemViewType) {
                        TYPE_HEADER -> {
                            val tv = holder.itemView as TextView
                            tv.text = item as String
                            applyFontSize(tv, getPrefs().fontSize, getPrefs().fontSize + 2)
                        }
                        TYPE_ITEM -> {
                            val entity = item as MeatEntity
                            val text1 = holder.itemView.findViewById<TextView>(R.id.text1)
                            val text2 = holder.itemView.findViewById<TextView>(R.id.text2)
                            val btnEdit = holder.itemView.findViewById<Button>(R.id.btnEdit)

                            text1.text = entity.entity
                            text2.text = ""

                            btnEdit.setOnClickListener { showEditEntityDialog(entity) }
                            applyFontSize(holder.itemView, getPrefs().fontSize)

                            holder.itemView.setOnLongClickListener {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Удалить")
                                    .setMessage("Удалить \"${entity.entity}\"?")
                                    .setPositiveButton("Да") { _, _ -> lifecycleScope.launch { dao.deleteEntity(entity) } }
                                    .setNegativeButton("Нет", null)
                                    .show()
                                true
                            }
                        }
                    }
                }

                override fun getItemCount() = flatList.size
            }
            binding.recyclerView.adapter = adapter
        }
    }

    private fun showEditEntityDialog(entity: MeatEntity) {
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val etName = EditText(requireContext()).apply { setText(entity.entity) }
        val etGroup = EditText(requireContext()).apply { setText(entity.group) }
        layout.addView(etName)
        layout.addView(etGroup)
        applyFontSize(layout, getPrefs().fontSize)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать позицию")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = etName.text.toString().trim()
                val newGroup = etGroup.text.toString().trim().ifEmpty { entity.group }
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        getDao().updateEntity(MeatEntity(id = entity.id, entity = newName, group = newGroup))
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
    }

    // ========== Шаблоны ==========
    private suspend fun setupTemplates(dao: AppDao) {
        dao.getAllTemplates().collectLatest { templates ->
            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_edit_button, parent, false)
                    return object : RecyclerView.ViewHolder(itemView) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val template = templates[position]
                    val text1 = holder.itemView.findViewById<TextView>(R.id.text1)
                    val text2 = holder.itemView.findViewById<TextView>(R.id.text2)
                    val btnEdit = holder.itemView.findViewById<Button>(R.id.btnEdit)

                    text1.text = template.temp
                    text2.text = ""

                    btnEdit.setOnClickListener { showEditTemplateDialog(template) }
                    applyFontSize(holder.itemView, getPrefs().fontSize)

                    holder.itemView.setOnClickListener {
                        val bundle = Bundle().apply {
                            putInt("templateId", template.id)
                            putString("templateName", template.temp)
                        }
                        findNavController().navigate(R.id.action_directoryEditFragment_to_templateEditFragment, bundle)
                    }

                    holder.itemView.setOnLongClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Удалить шаблон")
                            .setMessage("Удалить \"${template.temp}\" и все его элементы?")
                            .setPositiveButton("Да") { _, _ ->
                                lifecycleScope.launch {
                                    dao.deleteTemplateItemsByTemplateId(template.id)
                                    dao.deleteTemplate(template)
                                }
                            }
                            .setNegativeButton("Нет", null)
                            .show()
                        true
                    }
                }

                override fun getItemCount() = templates.size
            }
            binding.recyclerView.adapter = adapter
        }
    }

    private fun showEditTemplateDialog(template: Template) {
        val etName = EditText(requireContext()).apply { setText(template.temp) }
        applyFontSize(etName, getPrefs().fontSize)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать название шаблона")
            .setView(etName)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        getDao().updateTemplate(Template(id = template.id, temp = newName))
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
    }

    // ========== Единицы измерения ==========
    private suspend fun setupInputTypes(dao: AppDao) {
        dao.getAllInputTypes().collectLatest { inputTypes ->
            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_edit_button, parent, false)
                    return object : RecyclerView.ViewHolder(itemView) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val type = inputTypes[position]
                    val text1 = holder.itemView.findViewById<TextView>(R.id.text1)
                    val text2 = holder.itemView.findViewById<TextView>(R.id.text2)
                    val btnEdit = holder.itemView.findViewById<Button>(R.id.btnEdit)

                    text1.text = type.type_name
                    text2.text = "Сокр.: ${type.short_name}, Вес: ${type.weight_kg} кг"

                    btnEdit.setOnClickListener { showEditInputTypeDialog(type) }
                    applyFontSize(holder.itemView, getPrefs().fontSize)
                }

                override fun getItemCount() = inputTypes.size
            }
            binding.recyclerView.adapter = adapter
        }
    }

    private fun showEditInputTypeDialog(type: InputType) {
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val etName = EditText(requireContext()).apply { setText(type.type_name) }
        val etShort = EditText(requireContext()).apply { setText(type.short_name) }
        val etWeight = EditText(requireContext()).apply { setText(type.weight_kg.toString()) }
        layout.addView(etName)
        layout.addView(etShort)
        layout.addView(etWeight)
        applyFontSize(layout, getPrefs().fontSize)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать единицу измерения")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = etName.text.toString().trim()
                val newShort = etShort.text.toString().trim()
                val newWeight = etWeight.text.toString().toDoubleOrNull() ?: type.weight_kg
                if (newName.isNotEmpty() && newShort.isNotEmpty()) {
                    lifecycleScope.launch {
                        getDao().updateInputType(InputType(id = type.id, type_name = newName, short_name = newShort, weight_kg = newWeight))
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
    }

    // ========== Паттерны ==========
    private suspend fun setupPatterns(dao: AppDao) {
        dao.getAllPatterns().collectLatest { patterns ->
            adapter = object : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_1, parent, false)
                    applyFontSize(itemView, getPrefs().fontSize)
                    return ViewHolder(itemView)
                }
                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val pattern = patterns[position]
                    holder.text1.text = pattern.name + if (pattern.is_active) " (активен)" else ""
                    holder.itemView.setOnClickListener { showEditPatternDialog(pattern) }
                    holder.itemView.setOnLongClickListener {
                        if (pattern.id == 1) {
                            Toast.makeText(requireContext(), "Базовый паттерн нельзя удалить", Toast.LENGTH_SHORT).show()
                        } else {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Удалить паттерн")
                                .setMessage("Удалить \"${pattern.name}\"?")
                                .setPositiveButton("Да") { _, _ -> lifecycleScope.launch { dao.deletePattern(pattern) } }
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

    // ========== Минимальный заказ ==========
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
            "min_order" -> {
                // Для минимального заказа используем тот же диалог, что и в остатках (через RemainsAdapter)
                // Кнопка FAB здесь не вызывает этот метод, т.к. в min_order свой адаптер
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
