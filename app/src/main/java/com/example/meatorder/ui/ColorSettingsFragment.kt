package com.example.meatorder.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentColorSettingsBinding
import com.example.meatorder.utils.applyFontSize
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
        applyFontSize(binding.root, getPrefs().fontSize)

        val prefs = getPrefs()

        binding.colorSettingsRoot.setOnClickListener {
            val current = prefs.headerColor
            val newColor = when (current) {
                Color.BLACK -> Color.RED
                Color.RED -> Color.BLUE
                else -> Color.BLACK
            }
            prefs.headerColor = newColor
            requireActivity().recreate()
        }

        binding.btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        val resetButton = android.widget.Button(requireContext()).apply {
            text = "Сбросить настройки цвета"
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Сбросить настройки?")
                    .setMessage("Вернуть цвета хедера, футера и фона к значениям по умолчанию?")
                    .setPositiveButton("Да") { _, _ ->
                        prefs.headerColor = Color.BLACK
                        prefs.footerColor = Color.WHITE
                        prefs.bodyColor = Color.WHITE
                        prefs.backgroundImagePath = null
                        requireActivity().recreate()
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        }
        (binding.root as? android.widget.LinearLayout)?.addView(resetButton)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
