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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.meatorder.data.dao.AppDao
import com.example.meatorder.data.entity.*
import com.example.meatorder.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DictionariesSettingsFragment : Fragment() {

    private lateinit var dao: AppDao
    private lateinit var prefs: PreferencesHelper

    // Лаунчер для выбора папки
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

    // Лаунчер для выбора ZIP-архива при импорте
    private val importZipLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importZipArchive(it) }
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
            setOnClickListener { importZipLauncher.launch(arrayOf("application/zip")) }
        }

        layout.addView(btnPath)
        layout.addView(btnExport)
        layout.addView(btnImport)

        return layout
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

    // ======== ИМПОРТ ИЗ ZIP ========
    private fun importZipArchive(zipUri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(zipUri) ?: return@launch
                val zip = ZipInputStream(inputStream)
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.endsWith(".csv")) {
                        val text = zip.bufferedReader().readText()
                        // Импортируем в базу
                        when (name) {
                            "entities.csv" -> importEntities(text)
                            "input_types.csv" -> importInputTypes(text)
                            "templates.csv" -> importTemplates(text)
                        }

                        // Обновляем файлы в выбранной папке, если она задана
                        val folderUriStr = this@DictionariesSettingsFragment.prefs.dictionariesFolderUri
                        if (!folderUriStr.isNullOrEmpty()) {
                            val folderUri = Uri.parse(folderUriStr)
                            val docFolder = DocumentFile.fromTreeUri(requireContext(), folderUri)
                            if (docFolder != null) {
                                val existingFile = docFolder.findFile(name)
                                existingFile?.delete()
                                val newFile = docFolder.createFile("text/csv", name)
                                newFile?.let {
                                    requireContext().contentResolver.openOutputStream(it.uri)?.use { out ->
                                        out.write(text.toByteArray())
                                    }
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                zip.close()
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

    // Вспомогательные методы импорта
    private suspend fun importEntities(csv: String) {
        val pairs = parseEntitiesCsv(csv)
        val entities = pairs.map { MeatEntity(entity = it.first, group = it.second) }
        dao.deleteAllEntities()
        dao.insertEntities(entities)
    }

    private suspend fun importInputTypes(csv: String) {
        val lines = csv.lines().drop(1).filter { it.isNotBlank() }
        val types = lines.map { line ->
            val parts = line.split(";")
            InputType(
                type_name = parts.getOrNull(0)?.trim() ?: "",
                short_name = parts.getOrNull(1)?.trim() ?: "",
                weight_kg = parts.getOrNull(2)?.toDoubleOrNull() ?: 1.0
            )
        }
        dao.deleteAllInputTypes()
        types.forEach { dao.insertInputType(it) }
    }

    private suspend fun importTemplates(csv: String) {
        val lines = csv.lines().drop(1).filter { it.isNotBlank() }
        val map = mutableMapOf<String, MutableList<TemplateItem>>()
        for (line in lines) {
            val parts = line.split(";")
            if (parts.size < 4) continue
            val tempName = parts[0].trim()
            val entityName = parts[1].trim()
            val inputType = parts[2].trim()
            val qty = parts[3].trim().toIntOrNull() ?: 0
            val entityId = dao.getAllEntities().first().find { it.entity == entityName }?.id ?: continue
            map.getOrPut(tempName) { mutableListOf() }
                .add(TemplateItem(entity_id = entityId, input_type = inputType, input_default = qty))
        }
        dao.deleteAllTemplates()
        dao.deleteAllTemplateItems()
        for ((name, items) in map) {
            val id = dao.insertTemplate(Template(temp = name))
            for (item in items) {
                dao.insertTemplateItem(item.copy(template_id = id.toInt()))
            }
        }
    }
}
