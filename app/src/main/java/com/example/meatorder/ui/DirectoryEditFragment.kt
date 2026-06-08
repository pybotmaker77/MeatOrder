package com.example.meatorder.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

    // Лаунчер для выбора файла импорта
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleImportFile(it) }
    }

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

        // Кнопка импорта
        val importButton = Button(requireContext()).apply {
            text = "Импорт"
            setOnClickListener {
                importFileLauncher.launch(arrayOf("*/*"))
            }
        }
        (binding.root as? LinearLayout)?.addView(importButton, 1)
        applyFontSize(importButton, getPrefs().fontSize)

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

    // ... остальные методы (setupEntities, setupTemplates, setupInputTypes, setupPatterns,
    // showAddDialog, showEditPatternDialog, ViewHolder) остаются без изменений.
    // Их полный код уже был в предыдущих полных версиях DirectoryEditFragment.kt,
    // я не привожу их здесь, чтобы не занимать место, но вы можете скопировать их из предыдущих ответов.
}
