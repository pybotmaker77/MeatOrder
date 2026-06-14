package com.example.meatorder.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.*
import com.example.meatorder.databinding.FragmentTemplateEditBinding
import com.example.meatorder.utils.applyFontSize
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TemplateEditFragment : Fragment() {
    private var _binding: FragmentTemplateEditBinding? = null
    private val binding get() = _binding!!
    private var templateId: Int = -1
    private var templateName: String = ""
    private var entities: List<MeatEntity> = emptyList()
    private var inputTypes: List<InputType> = emptyList()

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplateEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        templateId = arguments?.getInt("templateId", -1) ?: -1
        templateName = arguments?.getString("templateName", "") ?: ""

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.title = templateName
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(getPrefs().headerColor)
        applyFontSize(binding.root, getPrefs().fontSize)

        binding.recyclerViewItems.layoutManager = LinearLayoutManager(requireContext())

        val dao = getDao()
        lifecycleScope.launch {
            entities = dao.getAllEntities().first()
            inputTypes = dao.getAllInputTypes().first()
            dao.getTemplateItems(templateId).collectLatest { items ->
                updateList(items)
            }
        }

        binding.fabAddItem.setOnClickListener { showAddItemDialog() }
    }

    private fun updateList(items: List<TemplateItem>) {
        val grouped = items.groupBy { item ->
            entities.find { it.id == item.entity_id }?.group ?: "Без группы"
        }
        val flatList = mutableListOf<Any>()
        for ((group, list) in grouped) {
            flatList.add(group)
            flatList.addAll(list)
        }

        binding.recyclerViewItems.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
                        val templateItem = item as TemplateItem
                        val entity = entities.find { it.id == templateItem.entity_id }
                        val text1 = holder.itemView.findViewById<TextView>(R.id.text1)
                        val text2 = holder.itemView.findViewById<TextView>(R.id.text2)
                        val btnEdit = holder.itemView.findViewById<Button>(R.id.btnEdit)

                        text1.text = entity?.entity ?: "???"
                        text2.text = "${templateItem.input_default} ${templateItem.input_type}"

                        btnEdit.setOnClickListener {
                            showEditItemDialog(templateItem)
                        }
                        holder.itemView.setOnLongClickListener {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Удалить элемент")
                                .setMessage("Удалить \"${entity?.entity}\" из шаблона?")
                                .setPositiveButton("Да") { _, _ ->
                                    lifecycleScope.launch { getDao().deleteTemplateItem(templateItem) }
                                }
                                .setNegativeButton("Нет", null)
                                .show()
                            true
                        }

                        applyFontSize(holder.itemView, getPrefs().fontSize)
                    }
                }
            }

            override fun getItemCount() = flatList.size
        }
    }

    private fun makeSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, getPrefs().fontSize.toFloat())
                view.setPadding(10, 10, 10, 10)
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflater = LayoutInflater.from(context)
                val dropView = inflater.inflate(R.layout.item_spinner_dropdown, parent, false)
                val text = dropView.findViewById<TextView>(R.id.text1)
                text.text = getItem(position)
                text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, getPrefs().fontSize.toFloat())
                return dropView
            }
        }
    }

    private fun showAddItemDialog() {
        if (entities.isEmpty() || inputTypes.isEmpty()) {
            Toast.makeText(requireContext(), "Сначала заполните справочники", Toast.LENGTH_SHORT).show()
            return
        }

        val groups = entities.map { it.group }.distinct().sorted()

        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        // Группа
        val spinnerGroup = Spinner(requireContext()).apply {
            adapter = makeSpinnerAdapter(groups)
        }

        // Список элементов (изначально пустой, обновится при выборе группы)
        val spinnerEntity = Spinner(requireContext())

        // Обновление списка элементов при смене группы
        spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGroup = groups[position]
                val filtered = entities.filter { it.group == selectedGroup }.map { it.entity }
                spinnerEntity.adapter = makeSpinnerAdapter(filtered)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Единицы измерения
        val spinnerType = Spinner(requireContext()).apply {
            adapter = makeSpinnerAdapter(inputTypes.map { it.type_name })
        }

        val etQuantity = EditText(requireContext()).apply {
            hint = "Количество"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(spinnerGroup)
        layout.addView(Space(requireContext()).apply { minimumHeight = 16 })
        layout.addView(spinnerEntity)
        layout.addView(Space(requireContext()).apply { minimumHeight = 16 })
        layout.addView(spinnerType)
        layout.addView(Space(requireContext()).apply { minimumHeight = 16 })
        layout.addView(etQuantity)
        applyFontSize(layout, getPrefs().fontSize)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Добавить позицию в шаблон")
            .setView(layout)
            .setPositiveButton("Добавить") { _, _ ->
                val entityName = (spinnerEntity.selectedItem as? String) ?: return@setPositiveButton
                val entityId = entities.find { it.entity == entityName }?.id ?: return@setPositiveButton
                val typeIndex = spinnerType.selectedItemPosition
                val qty = etQuantity.text.toString().toIntOrNull() ?: 0
                if (typeIndex >= 0 && qty > 0) {
                    val inputTypeName = inputTypes[typeIndex].type_name
                    lifecycleScope.launch {
                        getDao().insertTemplateItem(
                            TemplateItem(
                                template_id = templateId,
                                entity_id = entityId,
                                input_type = inputTypeName,
                                input_default = qty
                            )
                        )
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
    }

    private fun showEditItemDialog(item: TemplateItem) {
        val currentEntity = entities.find { it.id == item.entity_id }
        val currentGroup = currentEntity?.group ?: ""
        val groups = entities.map { it.group }.distinct().sorted()

        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val spinnerGroup = Spinner(requireContext()).apply {
            adapter = makeSpinnerAdapter(groups)
            val groupIndex = groups.indexOf(currentGroup)
            if (groupIndex >= 0) setSelection(groupIndex)
        }

        val spinnerEntity = Spinner(requireContext())
        spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGroup = groups[position]
                val filtered = entities.filter { it.group == selectedGroup }.map { it.entity }
                spinnerEntity.adapter = makeSpinnerAdapter(filtered)
                val entityIndex = filtered.indexOf(currentEntity?.entity ?: "")
                if (entityIndex >= 0) spinnerEntity.setSelection(entityIndex)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val currentTypeIndex = inputTypes.indexOfFirst { it.type_name == item.input_type }
        val spinnerType = Spinner(requireContext()).apply {
            adapter = makeSpinnerAdapter(inputTypes.map { it.type_name })
            if (currentTypeIndex >= 0) setSelection(currentTypeIndex)
        }

        val etQuantity = EditText(requireContext()).apply {
            setText(item.input_default.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(spinnerGroup)
        layout.addView(Space(requireContext()).apply { minimumHeight = 16 })
        layout.addView(spinnerEntity)
        layout.addView(Space(requireContext()).apply { minimumHeight = 16 })
        layout.addView(spinnerType)
        layout.addView(Space(requireContext()).apply { minimumHeight = 16 })
        layout.addView(etQuantity)
        applyFontSize(layout, getPrefs().fontSize)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать позицию")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                val entityName = (spinnerEntity.selectedItem as? String) ?: return@setPositiveButton
                val entityId = entities.find { it.entity == entityName }?.id ?: return@setPositiveButton
                val typeIndex = spinnerType.selectedItemPosition
                val qty = etQuantity.text.toString().toIntOrNull() ?: item.input_default
                if (typeIndex >= 0 && qty > 0) {
                    lifecycleScope.launch {
                        getDao().updateTemplateItem(
                            TemplateItem(
                                id = item.id,
                                template_id = item.template_id,
                                entity_id = entityId,
                                input_type = inputTypes[typeIndex].type_name,
                                input_default = qty
                            )
                        )
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
