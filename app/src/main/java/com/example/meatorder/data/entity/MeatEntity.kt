package com.example.meatorder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entities")
data class MeatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entity: String,
    val group: String
)