package org.jetbrains.compose.common.internal

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.css.StyleBuilder
import org.jetbrains.compose.web.attributes.AttrsBuilder

@ExperimentalComposeWebWidgets
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

@ExperimentalComposeWebWidgets
fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
