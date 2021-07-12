package org.jetbrains.compose.common.internal

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.css.StylePropertyBuilder
import org.jetbrains.compose.web.attributes.AttrsBuilder

class ActualModifier : Modifier {
    val styleHandlers = mutableListOf<StylePropertyBuilder.() -> Unit>()
    val attrHandlers = mutableListOf<AttrsBuilder<*>.() -> Unit>()

    fun add(builder: StylePropertyBuilder.() -> Unit) {
        styleHandlers.add(builder)
    }

    fun addAttributeBuilder(builder: AttrsBuilder<*>.() -> Unit) {
        attrHandlers.add(builder)
    }
}

fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
