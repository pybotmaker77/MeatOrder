package com.example.meatorder.ui

import com.example.meatorder.data.entity.InputType

data class Order3Item(
    val entityId: Int,
    val entity: String,
    val group: String,
    val inputType: InputType,
    val quantity: Int
)
