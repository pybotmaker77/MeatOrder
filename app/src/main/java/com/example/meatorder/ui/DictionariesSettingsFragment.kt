package com.example.meatorder.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.meatorder.data.dao.AppDao
import com.example.meatorder.data.entity.*
import com.example.meatorder.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DictionariesSettingsFragment : Fragment() {

    private lateinit var dao: AppDao
    private lateinit var prefs: PreferencesHelper

    // Лаунчеры пока без реальной логики, чтобы избежать ошибок компиляции
    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        // Будет реализовано позже
    }

    private val importZipLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Будет реализовано позже
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация dao и prefs
        dao = getDao()
        prefs = getPrefs()

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
