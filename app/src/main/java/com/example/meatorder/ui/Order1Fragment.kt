package com.example.meatorder.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.data.entity.Template
import com.example.meatorder.databinding.FragmentOrder1Binding
import com.example.meatorder.utils.applyFontSize
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Order1Fragment : Fragment() {
    private var _binding: FragmentOrder1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrder1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(getPrefs().headerColor)
        applyFontSize(binding.root, getPrefs().fontSize)

        binding.btnBalance.setOnClickListener {
            findNavController().navigate(R.id.action_order1Fragment_to_remainsFragment)
        }

        binding.btnFromScratch.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("byBalance", false)
                putIntArray("templateIds", intArrayOf())
            }
            findNavController().navigate(R.id.action_order1Fragment_to_order2Fragment, bundle)
        }

        binding.btnFromTemplate.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val templates = getDao().getAllTemplates().first()
                withContext(Dispatchers.Main) {
                    showTemplateDialog(templates)
                }
            }
        }

        binding.btnTemplates.setOnClickListener {
            Toast.makeText(requireContext(), "Управление шаблонами (в разработке)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTemplateDialog(templates: List<Template>) {
        if (templates.isEmpty()) {
            Toast.makeText(requireContext(), "Нет сохранённых шаблонов", Toast.LENGTH_SHORT).show()
            return
        }
        val names = templates.map { it.temp }.toTypedArray()
        val checked = BooleanArray(templates.size) { false }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Выберите шаблоны")
            .setMultiChoiceItems(names, checked) { _, index, isChecked ->
                checked[index] = isChecked
            }
            .setPositiveButton("Выбрать") { _, _ ->
                val selectedIds = templates.filterIndexed { index, _ -> checked[index] }
                    .map { it.id }.toIntArray()
                val bundle = Bundle().apply {
                    putBoolean("byBalance", false)
                    putIntArray("templateIds", selectedIds)
                }
                findNavController().navigate(R.id.action_order1Fragment_to_order2Fragment, bundle)
            }
            .setNegativeButton("Отмена", null)
            .show()

        // Применяем шрифт ко всем элементам диалога
        applyFontSize(dialog.window?.decorView ?: return, getPrefs().fontSize)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { applyFontSize(it, getPrefs().fontSize) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
