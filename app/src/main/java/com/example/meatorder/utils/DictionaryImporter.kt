package com.example.meatorder.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.meatorder.data.dao.AppDao
import com.example.meatorder.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DictionaryImporter {

    suspend fun importZipArchive(context: Context, zipUri: Uri, dao: AppDao, folderUri: Uri?) {
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(zipUri) ?: return@withContext
            val zip = ZipInputStream(inputStream)
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.endsWith(".csv")) {
                    val text = zip.bufferedReader().readText()
                    when (name) {
                        "entities.csv" -> importEntities(text, dao)
                        "input_types.csv" -> importInputTypes(text, dao)
                        "templates.csv" -> importTemplates(text, dao)
                    }

                    // Обновляем файлы в папке, если она задана
                    folderUri?.let { uri ->
                        val docFolder = DocumentFile.fromTreeUri(context, uri)
                        if (docFolder != null) {
                            val existingFile = docFolder.findFile(name)
                            existingFile?.delete()
                            val newFile = docFolder.createFile("text/csv", name)
                            newFile?.let {
                                context.contentResolver.openOutputStream(it.uri)?.use { out ->
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
        }
    }

    private suspend fun importEntities(csv: String, dao: AppDao) {
        val pairs = parseEntitiesCsv(csv)
        val entities = pairs.map { MeatEntity(entity = it.first, group = it.second) }
        dao.deleteAllEntities()
        dao.insertEntities(entities)
    }

    private suspend fun importInputTypes(csv: String, dao: AppDao) {
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

    private suspend fun importTemplates(csv: String, dao: AppDao) {
        val lines = csv.lines().drop(1).filter { it.isNotBlank() }
        val map = mutableMapOf<String, MutableList<TemplateItem>>()
        // Загружаем все entities один раз
        val entities = dao.getAllEntities().first()
        for (line in lines) {
            val parts = line.split(";")
            if (parts.size < 4) continue
            val tempName = parts[0].trim()
            val entityName = parts[1].trim()
            val inputType = parts[2].trim()
            val qty = parts[3].trim().toIntOrNull() ?: 0
            val entityId = entities.find { it.entity == entityName }?.id ?: continue
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
