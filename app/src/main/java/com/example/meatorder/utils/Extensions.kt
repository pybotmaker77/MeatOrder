package com.example.meatorder.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.example.meatorder.MeatOrderApp
import java.io.BufferedReader
import java.io.InputStreamReader

fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Fragment.getApp(): MeatOrderApp = requireActivity().application as MeatOrderApp
fun Fragment.getDao() = getApp().database.dao()
fun Fragment.getPrefs() = getApp().prefs

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
    return lines.drop(1).map { line ->
        val parts = line.split(";")
        if (parts.size >= 2) Pair(parts[0], parts[1])
        else Pair(parts[0], "Без группы")
    }
}
