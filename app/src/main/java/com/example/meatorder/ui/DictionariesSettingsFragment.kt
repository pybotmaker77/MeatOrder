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
import java.util.zip.ZipOutputStream

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dao = getDao()
        prefs = getPrefs()

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

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
            setOnClickListener {
                Toast.makeText(requireContext(), "Функция в разработке", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(btnPath)
        layout.addView(btnExport)
        layout.addView(btnImport)

        return layout
    }

    private fun exportFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val entities = dao.getAllEntities().first()
                val inputTypes = dao.getAllInputTypes().first()
                val templates = dao.getAllTemplates().first()
                val templateItemsMap = mutableMapOf<Int, List<TemplateItem>>()
                for (t in templates) {
                    templateItemsMap[t.id] = dao.getTemplateItems(t.id).first()
                }

                val zipFile = File(requireContext().cacheDir, "справочники.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    zos.putNextEntry(ZipEntry("entities.csv"))
                    zos.write("entity;group\n".toByteArray())
                    for (e in entities) {
                        zos.write("${e.entity};${e.group}\n".toByteArray())
                    }
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("input_types.csv"))
                    zos.write("type_name;short_name;weight_kg\n".toByteArray())
                    for (t in inputTypes) {
                        zos.write("${t.type_name};${t.short_name};${t.weight_kg}\n".toByteArray())
                    }
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("templates.csv"))
                    zos.write("temp;entity;input_type;input_default\n".toByteArray())
                    for (t in templates) {
                        val items = templateItemsMap[t.id] ?: emptyList()
                        for (item in items) {
                            val entityName = entities.find { it.id == item.entity_id }?.entity ?: ""
                            zos.write("${t.temp};$entityName;${item.input_type};${item.input_default}\n".toByteArray())
                        }
                    }
                    zos.closeEntry()
                }

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    zipFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    startActivity(Intent.createChooser(shareIntent, "Отправить справочники"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
