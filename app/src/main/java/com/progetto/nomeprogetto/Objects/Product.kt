package com.progetto.nomeprogetto.Objects

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
@Parcelize
class Product(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val width: Double,
    val height: Double,
    val length: Double,
    val main_picture: Bitmap?,
    val avgRating: Double,
    val reviewsNumber: Int,
    val uploadDate: LocalDateTime,
    //per il carrello:
    val itemId: Int? = null,
    val colorName: String? = null,
    val color_hex: String? = null,
    var quantity: Int? = null,
    val stock: Int? = null,
    val picture: Bitmap? = null,
    val colorId: Int? = null,
    val category: String? = null,
    val discount: Int? = null,
    val discount_desc: String? = null,
    val discounted_price: Double = price
) : Parcelable