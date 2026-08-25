package com.lushaiedupls.ui.parent

import java.text.NumberFormat
import java.util.Locale

fun formatInrFromPaise(paise: Int): String {
    val rupees = paise / 100
    val formatted = NumberFormat.getNumberInstance(Locale("en", "IN")).format(rupees)
    return "₹$formatted"
}

fun formatIsoDate(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return value.take(10)
}
