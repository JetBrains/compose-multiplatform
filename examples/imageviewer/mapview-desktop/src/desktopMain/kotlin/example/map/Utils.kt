package example.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun Offset.toPt(): DisplayPoint = DisplayPoint(ceil(x).roundToInt(), ceil(y).roundToInt())
fun Offset.distanceTo(other: Offset): Double {
    val dx = other.x - x
    val dy = other.y - y
    return sqrt(dx * dx + dy * dy).toDouble()
}
