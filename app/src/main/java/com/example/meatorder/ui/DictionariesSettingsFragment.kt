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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.data.dao.AppDao
import com.example.meatorder.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictionariesSettingsFragment : Fragment() {

    private lateinit var dao: AppDao
    private lateinit var prefs: PreferencesHelper

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.dictionariesFolderUri = it.toString()
            Toast.makeText(requireContext(), "Папка выбрана. Экспорт CSV...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                DictionarySync.exportAllToFolder(requireContext(), it, dao)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Справочники экспортированы в папку", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val importZipLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importZipArchive(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dao = getDao()
        prefs = getPrefs()

        val view = inflater.inflate(R.layout.fragment_dictionaries_settings, container, false)

        val header = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(prefs.headerColor)

        val containerLayout = view.findViewById<LinearLayout>(R.id.container)

        val btnPath = Button(requireContext()).apply {
            text = "ПУТЬ К ФАЙЛАМ"
            setOnClickListener { folderPickerLauncher.launch(null) }
        }
        val btnExport = Button(requireContext()).apply {
            text = "ЭКСПОРТ ФАЙЛОВ"
            setOnClickListener { exportFiles() }
        }
        val btnImport = Button(requireContext()).apply {
            text = "ИМПОРТ ФАЙЛОВ"
            setOnClickListener { importZipLauncher.launch(arrayOf("application/zip")) }
        }

        containerLayout.addView(btnPath)
        containerLayout.addView(btnExport)
        containerLayout.addView(btnImport)

        applyFontSize(view, prefs.fontSize)

        return view
    }

    private fun exportFiles() {
        lifecycleScope.launch {
            try {
                DictionarySync.createAndShareZip(requireContext(), dao)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importZipArchive(zipUri: Uri) {
        lifecycleScope.launch {
            try {
                val folderUriStr = prefs.dictionariesFolderUri
                val folderUri = if (!folderUriStr.isNullOrEmpty()) Uri.parse(folderUriStr) else null
                DictionaryImporter.importZipArchive(requireContext(), zipUri, dao, folderUri)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Импорт завершён", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
