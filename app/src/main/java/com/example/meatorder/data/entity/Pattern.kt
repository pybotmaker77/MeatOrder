package com.example.meatorder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patterns")
data class Pattern(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val template: String,
    val is_active: Boolean = false
)