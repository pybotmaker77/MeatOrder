package com.example.meatorder.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.data.entity.MeatEntity
import com.example.meatorder.data.entity.TemplateItem
import com.example.meatorder.databinding.FragmentTemplateEditBinding
import com.example.meatorder.utils.getDao
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
        
        binding.recyclerViewItems.layoutManager = LinearLayoutManager(requireContext())

        val dao = getDao()
        lifecycleScope.launch {
            entities = dao.getAllEntities().first()
            inputTypes = dao.getAllInputTypes().first()

            // Загружаем элементы шаблона и подписываемся на изменения
            dao.getTemplateItems(templateId).collectLatest { items ->
                updateList(items)
            }
        }

        binding.fabAddItem.setOnClickListener { showAddItemDialog() }
    }

    private fun updateList(items: List<TemplateItem>) {
        binding.recyclerViewItems.adapter = object : RecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                return ViewHolder(itemView)
            }
            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val item = items[position]
                val entity = entities.find { it.id == item.entity_id }
                holder.text1.text = entity?.entity ?: "???"
                holder.text2?.text = "${item.input_default} ${item.input_type}"
                holder.itemView.setOnLongClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Удалить элемент")
                        .setMessage("Удалить \"${entity?.entity}\" из шаблона?")
                        .setPositiveButton("Да") { _, _ ->
                            lifecycleScope.launch {
                                getDao().deleteTemplateItem(item)
                            }
                        }
                        .setNegativeButton("Нет", null)
                        .show()
                    true
                }
            }
            override fun getItemCount() = items.size
        }
    }

    private fun showAddItemDialog() {
        if (entities.isEmpty() || inputTypes.isEmpty()) {
            Toast.makeText(requireContext(), "Сначала заполните справочники", Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val spinnerEntity = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, entities.map { it.entity })
        }
        val spinnerType = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, inputTypes.map { it.type_name })
        }
        val etQuantity = EditText(requireContext()).apply {
            hint = "Количество"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(spinnerEntity)
        layout.addView(spinnerType)
        layout.addView(etQuantity)

        AlertDialog.Builder(requireContext())
            .setTitle("Добавить позицию в шаблон")
            .setView(layout)
            .setPositiveButton("Добавить") { _, _ ->
                val entityIndex = spinnerEntity.selectedItemPosition
                val typeIndex = spinnerType.selectedItemPosition
                val qty = etQuantity.text.toString().toIntOrNull() ?: 0
                if (entityIndex >= 0 && typeIndex >= 0 && qty > 0) {
                    val entityId = entities[entityIndex].id
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
