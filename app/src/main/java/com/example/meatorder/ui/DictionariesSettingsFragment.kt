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

    // Локальные ссылки на DAO и настройки для использования в корутинах
    private val dao by lazy { getDao() }
    private val prefs by lazy { getPrefs() }

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.dictionariesFolderUri = it.toString()
            Toast.makeText(requireContext(), "Папка выбрана. Все изменения будут сохраняться в CSV.", Toast.LENGTH_SHORT).show()
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
                val zipFile = File(requireContext().cacheDir, "справочники.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    val entities = dao.getAllEntities().first()
                    zos.putNextEntry(ZipEntry("entities.csv"))
                    zos.write("entity;group\n".toByteArray())
                    for (e in entities) {
                        zos.write("${e.entity};${e.group}\n".toByteArray())
                    }
                    zos.closeEntry()

                    val inputTypes = dao.getAllInputTypes().first()
                    zos.putNextEntry(ZipEntry("input_types.csv"))
                    zos.write("type_name;short_name;weight_kg\n".toByteArray())
                    for (t in inputTypes) {
                        zos.write("${t.type_name};${t.short_name};${t.weight_kg}\n".toByteArray())
                    }
                    zos.closeEntry()

                    val templates = dao.getAllTemplates().first()
                    zos.putNextEntry(ZipEntry("templates.csv"))
                    zos.write("temp;entity;input_type;input_default\n".toByteArray())
                    for (t in templates) {
                        val items = dao.getTemplateItems(t.id).first()
                        for (item in items) {
                            val entity = entities.find { it.id == item.entity_id }?.entity ?: ""
                            zos.write("${t.temp};$entity;${item.input_type};${item.input_default}\n".toByteArray())
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
                startActivity(Intent.createChooser(shareIntent, "Отправить справочники"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                        when (name) {
                            "entities.csv" -> importEntities(text)
                            "input_types.csv" -> importInputTypes(text)
                            "templates.csv" -> importTemplates(text)
                        }

                        val folderUriStr = prefs.dictionariesFolderUri
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
                Toast.makeText(requireContext(), "Импорт завершён", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
