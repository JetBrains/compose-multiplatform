/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test.utils

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
internal fun DpOffset.toCGPoint(): CValue<CGPoint> = CGPointMake(x.value.toDouble(), y.value.toDouble())

internal fun DpRect.center(): DpOffset = DpOffset((left + right) / 2, (top + bottom) / 2)

internal fun DpRect.toRect(density: Density): Rect = Rect(
    left = left.value * density.density,
    right = right.value * density.density,
    top = top.value * density.density,
    bottom = bottom.value * density.density
)

internal fun Rect.toDpRect(density: Density): DpRect = DpRect(
    left = left.dp / density.density,
    right = right.dp / density.density,
    top = top.dp / density.density,
    bottom = bottom.dp / density.density
)

internal fun DpRectZero() = DpRect(0.dp, 0.dp, 0.dp, 0.dp)

internal fun DpRect.intersect(other: DpRect): DpRect {
    if (right < other.left || other.right < left) return DpRectZero()
    if (bottom < other.top || other.bottom < top) return DpRectZero()
    return DpRect(
        left = max(left, other.left),
        top = max(top, other.top),
        right = min(right, other.right),
        bottom = min(bottom, other.bottom)
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun CValue<CGPoint>.toDpOffset(): DpOffset = useContents { DpOffset(x.dp, y.dp) }

@OptIn(ExperimentalForeignApi::class)
internal fun UIView.dpRectInWindow() = convertRect(bounds, toView = null).toDpRect()
internal fun<T> List<T>.forEachWithPrevious(block: (T, T) -> Unit) {
    var previous: T? = null
    for (current in this) {
        previous?.let { block(it, current) }
        previous = current
    }
}
