package com.example.meatorder.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentColorSettingsBinding
import com.example.meatorder.utils.PreferencesHelper
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

    private val colorPalette = arrayOf(
        Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
        Color.MAGENTA, Color.CYAN, Color.GRAY, Color.DKGRAY, Color.LTGRAY,
        Color.WHITE, Color.parseColor("#FFA500"), Color.parseColor("#800080"), Color.parseColor("#A52A2A")
    )

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
        header?.setBackgroundColor(getPrefs().headerColor)
        header?.title = "Цвета темы"

        val prefs = getPrefs()
        updateColorLabel(prefs.headerColor)

        binding.btnPickColor.setOnClickListener {
            showColorPickerDialog(prefs)
        }

        binding.btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        binding.btnResetColors.setOnClickListener {
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

    private fun updateColorLabel(color: Int) {
        val colorName = when (color) {
            Color.BLACK -> "чёрный"
            Color.RED -> "красный"
            Color.BLUE -> "синий"
            Color.GREEN -> "зелёный"
            Color.YELLOW -> "жёлтый"
            Color.MAGENTA -> "пурпурный"
            Color.CYAN -> "голубой"
            Color.GRAY -> "серый"
            Color.DKGRAY -> "тёмно-серый"
            Color.LTGRAY -> "светло-серый"
            Color.WHITE -> "белый"
            Color.parseColor("#FFA500") -> "оранжевый"
            Color.parseColor("#800080") -> "фиолетовый"
            Color.parseColor("#A52A2A") -> "коричневый"
            else -> "пользовательский"
        }
        binding.tvCurrentColor.text = "Текущий цвет хедера: $colorName"
    }

    private fun showColorPickerDialog(prefs: PreferencesHelper) {
        val gridView = GridView(requireContext()).apply {
            numColumns = 4
            adapter = object : BaseAdapter() {
                override fun getCount() = colorPalette.size
                override fun getItem(position: Int) = colorPalette[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val view: View = convertView ?: View(requireContext())
                    view.setBackgroundColor(colorPalette[position])
                    view.minimumHeight = 80
                    view.minimumWidth = 80
                    return view
                }
            }
            setOnItemClickListener { _, _, position, _ ->
                val newColor = colorPalette[position]
                prefs.headerColor = newColor
                updateColorLabel(newColor)
                requireActivity().recreate()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Выберите цвет хедера")
            .setView(gridView)
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
