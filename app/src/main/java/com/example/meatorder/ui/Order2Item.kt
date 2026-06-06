package com.example.meatorder.ui

import com.example.meatorder.data.entity.InputType
import com.example.meatorder.data.entity.MeatEntity

data class Order2Item(
    val entity: MeatEntity?,
    val group: String?,
    var selected: Boolean = false,
    var inputType: InputType? = null,
    var quantity: Int = 0
)
