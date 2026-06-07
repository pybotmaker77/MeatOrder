package com.example.meatorder.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.example.meatorder.MeatOrderApp
import com.example.meatorder.data.entity.Template
import com.example.meatorder.data.entity.TemplateItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Fragment.getApp() = requireActivity().application as MeatOrderApp
fun Fragment.getDao() = (requireActivity().application as MeatOrderApp).database.dao()
fun Fragment.getPrefs() = (requireActivity().application as MeatOrderApp).prefs

fun Fragment.pickFile(launcher: ActivityResultLauncher<Intent>, mimeType: String = "*/*") {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
    }
    launcher.launch(intent)
}

fun Activity.pickFile(launcher: ActivityResultLauncher<Intent>, mimeType: String = "*/*") {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
    }
    launcher.launch(intent)
}

fun readTextFromUri(uri: Uri, activity: Activity): String {
    val inputStream = activity.contentResolver.openInputStream(uri)
    val reader = BufferedReader(InputStreamReader(inputStream))
    return reader.readText()
}

fun parseEntitiesCsv(text: String): List<Pair<String, String>> {
    val lines = text.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()
    // первая строка — заголовок, пропускаем
    return lines.drop(1).map { line ->
        val parts = line.split(";")
        if (parts.size >= 2) Pair(parts[0].trim(), parts[1].trim())
        else Pair(parts[0].trim(), "Без группы")
    }
}

data class ImportTemplate(
    val name: String,
    val items: List<ImportTemplateItem>
)

data class ImportTemplateItem(
    val entity: String,
    val input_type: String,
    val input_default: Int
)

fun parseTemplatesJson(json: String): List<Template> {
    val type = object : TypeToken<List<ImportTemplate>>() {}.type
    val importList: List<ImportTemplate> = Gson().fromJson(json, type)
    return importList.map { import ->
        Template(temp = import.name)
    }
}
