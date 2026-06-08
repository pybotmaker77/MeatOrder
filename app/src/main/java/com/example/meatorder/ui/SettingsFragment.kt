package com.example.meatorder.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentSettingsBinding
import com.example.meatorder.utils.getPrefs
import java.io.BufferedReader
import java.io.InputStreamReader

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Лаунчер для экспорта
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            val json = getPrefs().exportSettings()
            requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(json.toByteArray())
                Toast.makeText(requireContext(), "Настройки экспортированы", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Лаунчер для импорта
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val inputStream = requireContext().contentResolver.openInputStream(it)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val json = reader.readText()
            reader.close()
            getPrefs().importSettings(json)
            Toast.makeText(requireContext(), "Настройки импортированы. Перезапустите приложение.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyFontSize(binding.root, getPrefs().fontSize)

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(getPrefs().headerColor)

        binding.btnFont.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_fontSettingsFragment)
        }
        binding.btnColors.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_colorSettingsFragment)
        }
        binding.btnDirectories.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_directoriesFragment)
        }
        binding.btnExport.setOnClickListener {
            exportLauncher.launch("meat_order_settings.json")
        }
        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
