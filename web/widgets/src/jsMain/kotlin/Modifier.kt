package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.Color.RGB
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.web.css.StyleBuilder
import org.jetbrains.compose.web.attributes.AttrsBuilder

actual fun Modifier.background(color: Color): Modifier = castOrCreate().apply {
    add {
        backgroundColor(RGB(color.red, color.green, color.blue))
    }
}

fun Modifier.asAttributeBuilderApplier(
    passThroughHandler: (AttrsBuilder<*>.() -> Unit)? = null
): AttrsBuilder<*>.() -> Unit =
    castOrCreate().let { modifier ->
        val st: AttrsBuilder<*>.() -> Unit = {
            modifier.attrHandlers.forEach { it.invoke(this) }

            style {
                modifier.styleHandlers.forEach { it.invoke(this) }
            }

            passThroughHandler?.invoke(this)
        }

        st
    }

actual fun Modifier.padding(all: Dp): Modifier = castOrCreate().apply {
    // yes, it's not a typo, what Modifier.padding does is actually adding margin
    add {
        margin(all.value.px)
    }
}
