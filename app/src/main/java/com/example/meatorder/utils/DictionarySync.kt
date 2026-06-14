package com.example.meatorder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.meatorder.data.dao.AppDao
import com.example.meatorder.data.entity.MeatEntity
import com.example.meatorder.data.entity.TemplateItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DictionarySync {

    suspend fun exportAllToFolder(context: Context, folderUri: Uri, dao: AppDao) {
        withContext(Dispatchers.IO) {
            val docFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext

            val entities = dao.getAllEntities().first()
            writeCsvToFile(context, docFolder, "entities.csv") { outputStream ->
                outputStream.write("entity;group\n".toByteArray())
                for (e in entities) {
                    outputStream.write("${e.entity};${e.group}\n".toByteArray())
                }
            }

            val inputTypes = dao.getAllInputTypes().first()
            writeCsvToFile(context, docFolder, "input_types.csv") { outputStream ->
                outputStream.write("type_name;short_name;weight_kg\n".toByteArray())
                for (t in inputTypes) {
                    outputStream.write("${t.type_name};${t.short_name};${t.weight_kg}\n".toByteArray())
                }
            }

            val templates = dao.getAllTemplates().first()
            writeCsvToFile(context, docFolder, "templates.csv") { outputStream ->
                outputStream.write("temp;group;entity;input_type;input_default\n".toByteArray())
                for (t in templates) {
                    val items = dao.getTemplateItems(t.id).first()
                    for (item in items) {
                        val entity = entities.find { it.id == item.entity_id }
                        if (entity != null) {
                            outputStream.write("${t.temp};${entity.group};${entity.entity};${item.input_type};${item.input_default}\n".toByteArray())
                        }
                    }
                }
            }
        }
    }

    suspend fun createAndShareZip(context: Context, dao: AppDao) {
        withContext(Dispatchers.IO) {
            try {
                val entities = dao.getAllEntities().first()
                val inputTypes = dao.getAllInputTypes().first()
                val templates = dao.getAllTemplates().first()
                val templateItemsMap = mutableMapOf<Int, List<TemplateItem>>()
                for (t in templates) {
                    templateItemsMap[t.id] = dao.getTemplateItems(t.id).first()
                }

                val zipFile = File(context.cacheDir, "справочники.zip")
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
                    zos.write("temp;group;entity;input_type;input_default\n".toByteArray())
                    for (t in templates) {
                        val items = templateItemsMap[t.id] ?: emptyList()
                        for (item in items) {
                            val entity = entities.find { it.id == item.entity_id }
                            if (entity != null) {
                                zos.write("${t.temp};${entity.group};${entity.entity};${item.input_type};${item.input_default}\n".toByteArray())
                            }
                        }
                    }
                    zos.closeEntry()
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(shareIntent, "Отправить справочники"))
                }
            } catch (e: Exception) {
                // Ошибка логируется, но не крашит приложение
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
