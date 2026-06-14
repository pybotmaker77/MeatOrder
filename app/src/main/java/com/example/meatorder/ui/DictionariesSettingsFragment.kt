package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class DictionariesSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val btnPath = Button(requireContext()).apply {
            text = "ПУТЬ К ФАЙЛАМ"
            setOnClickListener {
                Toast.makeText(requireContext(), "Функция в разработке", Toast.LENGTH_SHORT).show()
            }
        }
        val btnExport = Button(requireContext()).apply {
            text = "ЭКСПОРТ ФАЙЛОВ"
            setOnClickListener {
                Toast.makeText(requireContext(), "Функция в разработке", Toast.LENGTH_SHORT).show()
            }
        }
        val btnImport = Button(requireContext()).apply {
            text = "ИМПОРТ ФАЙЛОВ"
            setOnClickListener {
                Toast.makeText(requireContext(), "Функция в разработке", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(btnPath)
        layout.addView(btnExport)
        layout.addView(btnImport)

        return layout
    }
}
