package com.progetto.nomeprogetto.Objects

import android.graphics.Bitmap
import java.time.LocalDate

class OrderItem (
    val orderId: Int,
    val name: String,
    val picture: Bitmap,
    val orderDate: LocalDate? = null,
    val price: Double? = null,
    val quantity: Int? = null,
    val originalPrice: Double? = null
)
