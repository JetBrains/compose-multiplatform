package com.example.jetsnack.ui.utils

import platform.Foundation.*

actual fun formatPrice(price: Long): String {
    val priceAsDouble = price / 100.0

    val formatter = NSNumberFormatter()
    formatter.setLocale(NSLocale.currentLocale)
    formatter.numberStyle = NSNumberFormatterCurrencyStyle

    val numberPrice = NSNumber.numberWithDouble(priceAsDouble)
    return formatter.stringFromNumber(numberPrice) ?: ""
}