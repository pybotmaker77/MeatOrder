package com.example.meatorder.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.meatorder.data.dao.AppDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object DictionarySync {

    suspend fun exportAllToFolder(context: Context, folderUri: Uri, dao: AppDao) {
        withContext(Dispatchers.IO) {
            val docFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext

            // Entities CSV
            val entities = dao.getAllEntities().first()
            writeCsvToFile(context, docFolder, "entities.csv") { outputStream ->
                outputStream.write("entity;group\n".toByteArray())
                for (e in entities) {
                    outputStream.write("${e.entity};${e.group}\n".toByteArray())
                }
            }

            // Input types CSV
            val inputTypes = dao.getAllInputTypes().first()
            writeCsvToFile(context, docFolder, "input_types.csv") { outputStream ->
                outputStream.write("type_name;short_name;weight_kg\n".toByteArray())
                for (t in inputTypes) {
                    outputStream.write("${t.type_name};${t.short_name};${t.weight_kg}\n".toByteArray())
                }
            }

            // Templates CSV
            val templates = dao.getAllTemplates().first()
            writeCsvToFile(context, docFolder, "templates.csv") { outputStream ->
                outputStream.write("temp;entity;input_type;input_default\n".toByteArray())
                for (t in templates) {
                    val items = dao.getTemplateItems(t.id).first()
                    for (item in items) {
                        val entity = entities.find { it.id == item.entity_id }?.entity ?: ""
                        outputStream.write("${t.temp};$entity;${item.input_type};${item.input_default}\n".toByteArray())
                    }
                }
            }
        }
    }

    private suspend fun writeCsvToFile(
        context: Context,
        folder: DocumentFile,
        fileName: String,
        writeContent: suspend (java.io.OutputStream) -> Unit
    ) {
        val existingFile = folder.findFile(fileName)
        existingFile?.delete()
        val newFile = folder.createFile("text/csv", fileName)
        newFile?.let {
            context.contentResolver.openOutputStream(it.uri)?.use { outputStream ->
                writeContent(outputStream)
            }
        }
    }
}
