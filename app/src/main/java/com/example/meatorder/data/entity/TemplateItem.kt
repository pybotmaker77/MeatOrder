package com.example.meatorder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_items",
    foreignKeys = [
        ForeignKey(
            entity = Template::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MeatEntity::class,
            parentColumns = ["id"],
            childColumns = ["entity_id"]
        )
    ]
)
data class TemplateItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val template_id: Int,
    val entity_id: Int,
    val input_type: String,
    val input_default: Int
)