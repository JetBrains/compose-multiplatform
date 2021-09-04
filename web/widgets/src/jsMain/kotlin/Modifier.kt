package org.jetbrains.compose.common.ui

import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.px

@ExperimentalComposeWebWidgetsApi
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

@ExperimentalComposeWebWidgetsApi
actual fun Modifier.padding(all: Dp): Modifier = castOrCreate().apply {
    // yes, it's not a typo, what Modifier.padding does is actually adding margin
    add {
        margin(all.value.px)
    }
}
