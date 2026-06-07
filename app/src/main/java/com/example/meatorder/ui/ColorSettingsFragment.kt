package com.example.meatorder.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.meatorder.databinding.FragmentColorSettingsBinding
import com.example.meatorder.utils.getPrefs

class ColorSettingsFragment : Fragment() {
    private var _binding: FragmentColorSettingsBinding? = null
    private val binding get() = _binding!!

    // Лаунчер для выбора изображения фона
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Сохраняем URI изображения
                getPrefs().backgroundImagePath = uri.toString()
                Toast.makeText(requireContext(), "Фоновое изображение выбрано", Toast.LENGTH_SHORT).show()
                // Для применения нужно перезапустить MainActivity, но пока просто покажем Toast
                // В реальном приложении можно перезапустить Activity или использовать тему
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

        val prefs = getPrefs()

        // Кнопка для смены цвета хедера (демонстрация: чёрный -> красный -> синий -> чёрный)
        binding.root.setOnClickListener {
            val current = prefs.headerColor
            when (current) {
                Color.BLACK -> prefs.headerColor = Color.RED
                Color.RED -> prefs.headerColor = Color.BLUE
                else -> prefs.headerColor = Color.BLACK
            }
            Toast.makeText(requireContext(), "Цвет хедера изменён. Перезапустите приложение, чтобы увидеть изменения.", Toast.LENGTH_LONG).show()
        }

        // Кнопка для выбора фонового изображения (появляется при долгом нажатии или добавим её программно)
        // Поскольку в макете только текст, добавим кнопку динамически
        val btnPickImage = Button(requireContext()).apply {
            text = "Выбрать фоновое изображение"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                pickImageLauncher.launch(intent)
            }
        }
        // Добавляем кнопку в контейнер (в fragment_color_settings.xml сейчас LinearLayout, заменим его на наш)
        (binding.root as? LinearLayout)?.addView(btnPickImage)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
