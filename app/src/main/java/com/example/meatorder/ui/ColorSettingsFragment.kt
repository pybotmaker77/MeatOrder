package com.example.meatorder.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentColorSettingsBinding
import com.example.meatorder.utils.getPrefs

class ColorSettingsFragment : Fragment() {
    private var _binding: FragmentColorSettingsBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                getPrefs().backgroundImagePath = uri.toString()
                Toast.makeText(requireContext(), "Фоновое изображение выбрано. Перезапустите приложение.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }

        val prefs = getPrefs()

        // Смена цвета хедера с немедленным применением
        binding.colorSettingsRoot.setOnClickListener {
            val current = prefs.headerColor
            val newColor = when (current) {
                Color.BLACK -> Color.RED
                Color.RED -> Color.BLUE
                else -> Color.BLACK
            }
            prefs.headerColor = newColor
            // Перезапускаем Activity, чтобы изменения вступили в силу
            requireActivity().recreate()
        }

        binding.btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
