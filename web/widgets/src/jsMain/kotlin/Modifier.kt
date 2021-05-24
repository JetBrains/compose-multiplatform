package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.core.graphics.Color
import androidx.compose.web.css.backgroundColor
import androidx.compose.web.css.margin
import androidx.compose.web.css.px
import androidx.compose.web.css.Color.RGB
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.web.css.StyleBuilder
import androidx.compose.web.attributes.AttrsBuilder

actual fun Modifier.background(color: Color): Modifier = castOrCreate().apply {
    add {
        backgroundColor(RGB(color.red, color.green, color.blue))
    }
}

fun Modifier.asStyleBuilderApplier(
    passThroughHandler: (StyleBuilder.() -> Unit)? = null
): StyleBuilder.() -> Unit = castOrCreate().let { modifier ->
    val st: StyleBuilder.() -> Unit = {
        modifier.styleHandlers.forEach { it.invoke(this) }
        passThroughHandler?.invoke(this)
    }

    st
}

fun Modifier.asAttributeBuilderApplier(
    passThroughHandler: (AttrsBuilder<*>.() -> Unit)? = null
): AttrsBuilder<*>.() -> Unit =
    castOrCreate().let { modifier ->
        val st: AttrsBuilder<*>.() -> Unit = {
            modifier.attrHandlers.forEach { it.invoke(this) }
            passThroughHandler?.invoke(this)
        }

        st
    }

actual fun Modifier.padding(all: Dp): Modifier = castOrCreate().apply {
    // yes, it's not a typo, what Modifier.padding does is actually adding marginEe
    add {
        margin(all.value.px)
    }
}