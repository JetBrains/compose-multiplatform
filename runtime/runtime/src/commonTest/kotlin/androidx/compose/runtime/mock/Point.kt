package androidx.compose.runtime.mock

data class Point(val x: Int, val y: Int) {
    override fun toString(): String = "($x, $y)"
}
