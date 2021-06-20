package org.jetbrains.compose.common.ui

import co.touchlab.compose.darwin.internal.castOrCreate
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.unit.Dp
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.layoutIfNeeded
import platform.UIKit.layoutMargins
import platform.UIKit.setBackgroundColor

actual fun Modifier.background(color: Color): Modifier {
    return castOrCreate().apply {
        add {
            setBackgroundColor(
                UIColor(
                    red = color.red.toDouble() / 255F,
                    green = color.green.toDouble() / 255F,
                    blue = color.blue.toDouble() / 255F,
                    alpha = 1.0,
                )
            )
        }
    }
}

actual fun Modifier.padding(all: Dp): Modifier = castOrCreate().apply {
    add {
        val dim = all.value.toDouble()
        println("adding padding $dim")
        layoutMargins = UIEdgeInsetsMake(dim, dim, dim, dim)
        layoutIfNeeded()
    }
}