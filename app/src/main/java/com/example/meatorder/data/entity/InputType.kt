package com.example.meatorder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "input_types")
data class InputType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type_name: String,
    val short_name: String,
    val weight_kg: Double
)