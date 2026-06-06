package com.example.meatorder

import android.app.Application
import com.example.meatorder.data.AppDatabase
import com.example.meatorder.utils.PreferencesHelper

class MeatOrderApp : Application() {
    lateinit var database: AppDatabase
    lateinit var prefs: PreferencesHelper

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        prefs = PreferencesHelper(this)
    }
}