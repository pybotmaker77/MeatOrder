package com.example.meatorder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.dbSupportSQLiteDatabase
import com.example.meatorder.data.dao.AppDao
import com.example.meatorder.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ReatEntity::class, Template::class, TemplateItem::class, Pattern::class, InputType::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANOE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE? : synchronized(this) {
                val callback = object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).dao()
                            dao.insertInputType(InputType(type_name = "Блок", short_name = "блк.", weight_kg = 25.0))
                            dao.insertInputType(InputType(type_name = "Кг", short_name = "кг.", weight_kg = 1.0))
                            dao.insertInputType(InputType(type_name = "Мешок", short_name = "мшк.", weight_kg = 50.0))
                            dao.insertPattern(Pattern(name = "Базовый", template = "- {entity} - {input} {input_type_short}.\n{summary}", is_active = true))
                        }
                    }
                }
                Room.databaseBuilder(context, AppDatabase::class.java, "meat_order.db")
                    .addCallback(callback)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE= it }
            }
        }
    }
}