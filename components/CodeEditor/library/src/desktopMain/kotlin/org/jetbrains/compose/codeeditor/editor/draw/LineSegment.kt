package org.jetbrains.compose.codeeditor.editor.draw

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
internal class LineSegment {
    val xl: Float
    val xr: Float
    val y: Float

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(xl: Float, xr: Float, y: Float) {
        this.xl = if (xl > xr) xr else xl
        this.xr = xr
        this.y = y
    }

    @Stable
    operator fun component1(): Float = xl

    @Stable
    operator fun component2(): Float = xr

    @Stable
    operator fun component3(): Float = y

    @Stable
    fun copy(xl: Float = this.xl, xr: Float = this.xr, y: Float = this.y) = LineSegment(xl, xr, y)
}
