package com.example.jetsnack.ui.utils


private external object Intl {
    class NumberFormat(locales: String, options: JsAny) {
        fun format(l: Double): String
    }
}

private fun formatAsUSD(): JsAny = js("({ style: 'currency', currency: 'USD',})")

private val formatter = Intl.NumberFormat("en-US", formatAsUSD())

actual fun formatPrice(price: Long): String {
    // price is represented in total amount of cents, so divide by 100
    return formatter.format(price / 100.0)
}