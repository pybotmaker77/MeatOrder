package com.example.meatorder.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("meat_order_prefs", Context.MODE_PRIVATE)

    var fontSize: Int
        get() = prefs.getInt("font_size", 14)
        set(value) = prefs.edit().putInt("font_size", value).apply()

    var headerColor: Int
        get() = prefs.getInt("header_color", 0xFF000000.toInt())
        set(value) = prefs.edit().putInt("header_color", value).apply()

    var footerColor: Int
        get() = prefs.getInt("footer_color", 0xFFFFFFFF.toInt())
        set(value) = prefs.edit().putInt("footer_color", value).apply()

    var bodyColor: Int
        get() = prefs.getInt("body_color", 0xFFFFFFFF.toInt())
        set(value) = prefs.edit().putInt("body_color", value).apply()

    var backgroundImagePath: String?
        get() = prefs.getString("bg_image_path", null)
        set(value) = prefs.edit().putString("bg_image_path", value).apply()

    var draftOrderJson: String?
        get() = prefs.getString("draft_order", null)
        set(value) = prefs.edit().putString("draft_order", value).apply()

    fun saveDraft(orderJson: String) {
        draftOrderJson = orderJson
    }

    fun clearDraft() {
        prefs.edit().remove("draft_order").apply()
    }

    fun exportSettings(): String {
        val map = mapOf(
            "fontSize" to fontSize,
            "headerColor" to headerColor,
            "footerColor" to footerColor,
            "bodyColor" to bodyColor,
            "bgImagePath" to (backgroundImagePath ?: "")
        )
        return Gson().toJson(map)
    }

    fun importSettings(json: String) {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = Gson().fromJson(json, type)
        fontSize = (map["fontSize"] as Double).toInt()
        headerColor = (map["headerColor"] as Double).toInt()
        footerColor = (map["footerColor"] as Double).toInt()
        bodyColor = (map["bodyColor"] as Double).toInt()
        backgroundImagePath = map["bgImagePath"] as? String
    }
}
