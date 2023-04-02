package com.map

fun InternalMapState.toShortString(): String = buildString {
    appendLine("width: $width, height: $height")
    appendLine("scale: ${scale.toShortString()}")
//    appendLine("zoom: $zoom")
    appendLine("lat: ${centerGeo.latitude.toShortString()}, lon: ${centerGeo.longitude.toShortString()}")
}

fun Double.toShortString(significantDigits: Int = 5): String {
    var multiplier: Long = 1
    repeat(significantDigits) {
        multiplier *= 10
    }
    val result = (this * multiplier).toLong().toDouble() / multiplier
    return result.toString()
}
