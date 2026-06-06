package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.data.entity.Template
import com.example.meatorder.databinding.FragmentOrder1Binding
import com.example.meatorder.utils.getDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        binding.btnBalance.setOnClickListener {
            findNavController().navigate(R.id.action_order1Fragment_to_remainsFragment)
        }

        binding.btnFromScratch.setOnClickListener {
            findNavController().navigate(R.id.action_order1Fragment_to_order2Fragment)
        }

        binding.btnFromTemplate.setOnClickListener {
            val dao = getDao()
            CoroutineScope(Dispatchers.IO).launch {
                val templates = dao.getAllTemplates().collect { list ->
                    withContext(Dispatchers.Main) {
                        showTemplateDialog(list)
                    }
                }
            }
        }

        binding.btnTemplates.setOnClickListener {
            findNavController().navigate(R.id.action_order1Fragment_to_directoriesFragment)
        }
    }

    private fun showTemplateDialog(templates: List<Template>) {
        val names = templates.map { it.temp }.toTypedArray()
        val checked = BooleanArray(templates.size) { false }
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите шаблоны")
            .setMultiChoiceItems(names, checked) { _, index, isChecked ->
                checked[index] = isChecked
            }
            .setPositiveButton("Выбрать") { _, _ ->
                val selectedIds = templates.filterIndexed { index, _ -> checked[index] }
                    .map { it.id }.toIntArray()
                findNavController().navigate(R.id.action_order1Fragment_to_order2Fragment)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
