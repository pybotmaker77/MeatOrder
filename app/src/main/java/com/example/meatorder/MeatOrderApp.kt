package com.example.meatorder

import android.app.Application
import com.example.meatorder.data.AppDatabase
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.data.entity.Pattern
import com.example.meatorder.utils.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MeatOrderApp : Application() {
    lateinit var database: AppDatabase
    lateinit var prefs: PreferencesHelper

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        prefs = PreferencesHelper(this)

        // При первом запуске, если единицы измерения отсутствуют, добавляем стандартные
        CoroutineScope(Dispatchers.IO).launch {
            val dao = database.dao()
            if (dao.getAllInputTypes().first().isEmpty()) {
                dao.insertInputType(InputType(type_name = "Блок", short_name = "блк.", weight_kg = 20.0))
                dao.insertInputType(InputType(type_name = "Кг", short_name = "кг.", weight_kg = 1.0))
                dao.insertInputType(InputType(type_name = "Мешок", short_name = "мшк.", weight_kg = 20.0))
                dao.insertPattern(Pattern(name = "Базовый", template = "- {entity} - {input} {input_type_short}.\n{summary}", is_active = true))
            }
        }
    }
}
