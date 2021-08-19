package org.jetbrains.compose.common.internal

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.css.StyleBuilder
import org.jetbrains.compose.web.attributes.AttrsBuilder

@ExperimentalComposeWebWidgetsApi
class ActualModifier : Modifier {
    val styleHandlers = mutableListOf<StyleBuilder.() -> Unit>()
    val attrHandlers = mutableListOf<AttrsBuilder<*>.() -> Unit>()

    fun add(builder: StyleBuilder.() -> Unit) {
        styleHandlers.add(builder)
    }

    fun addAttributeBuilder(builder: AttrsBuilder<*>.() -> Unit) {
        attrHandlers.add(builder)
    }
}

@ExperimentalComposeWebWidgetsApi
fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
