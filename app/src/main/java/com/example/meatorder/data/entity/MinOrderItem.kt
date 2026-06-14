package com.example.meatorder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "min_order_items")
data class MinOrderItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entity_id: Int,
    val input_type: String,
    val quantity: Int
)
