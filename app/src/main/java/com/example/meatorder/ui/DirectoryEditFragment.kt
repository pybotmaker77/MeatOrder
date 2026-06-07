package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R

class DirectoryEditFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Простейший макет с текстом
        val textView = TextView(requireContext()).apply {
            text = "Редактор справочника (в разработке)\nТип: ${arguments?.getString("dict") ?: ""}"
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
        return textView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Добавляем возможность вернуться назад через аппаратную кнопку или программно
        // Здесь нет хедера, но стрелка назад в системной панели работает
    }
}
