package com.example.meatorder.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.meatorder.utils.getPrefs
import com.example.meatorder.databinding.FragmentColorSettingsBinding

class ColorSettingsFragment : Fragment() {
    private var _binding: FragmentColorSettingsBinding? = null
    private val binding get() = _binding!!

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

        // Простейший выбор цвета для хедера (заглушка, можно расширить позже)
        binding.root.setOnClickListener {
            // Переключение между чёрным и красным для демонстрации
            val current = prefs.headerColor
            if (current == Color.BLACK) {
                prefs.headerColor = Color.RED
            } else {
                prefs.headerColor = Color.BLACK
            }
            // Применение изменений потребует пересоздания Activity, пока просто покажем Toast
            android.widget.Toast.makeText(requireContext(), "Цвет хедера изменён. Перезапустите приложение.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
