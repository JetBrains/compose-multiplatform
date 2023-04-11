package example.map

import kotlin.math.*

/**
 * GeoPoint in relative geo coordinated
 * x in range 0f to 1f (equals to longitude -180 .. 180)
 * y in range 0 to 1  (equals to latitude 90 .. -90)
 */
data class GeoPoint(val x: Double, val y: Double)

/**
 * DisplayPoint screen coordinates (Also it may be used as distance between 2 screen point)
 */
data class DisplayPoint(val x: Int, val y: Int)

val GeoPoint.longitude get(): Double = x * 360.0 - 180.0
val GeoPoint.latitude
    get(): Double {
        val latRad = atan(sinh(PI * (1 - 2 * y)))
        return latRad / PI * 180.0
    }

fun createGeoPt(latitude: Double, longitude: Double): GeoPoint {
    val x = (longitude + 180) / 360
    val y = (1 - ln(tan(latitude.toRad()) + 1 / cos(latitude.toRad())) / PI) / 2
    return GeoPoint(x, y)
}

fun Double.toRad() = this * PI / 180

operator fun DisplayPoint.minus(other: DisplayPoint): DisplayPoint =
    DisplayPoint(this.x - other.x, this.y - other.y)

@Suppress("unused")
fun DisplayPoint.distanceTo(other: DisplayPoint): Double {
    val dx = other.x - x
    val dy = other.y - y
    return sqrt(dx * dx.toDouble() + dy * dy.toDouble())
}

operator fun GeoPoint.minus(minus: GeoPoint): GeoPoint {
    return GeoPoint(x - minus.x, y - minus.y)
}

operator fun GeoPoint.plus(other: GeoPoint): GeoPoint {
    return GeoPoint(x + other.x, y + other.y)
}
