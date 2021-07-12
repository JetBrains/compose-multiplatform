package org.jetbrains.compose.common.internal

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.css.StylePropertyBuilder
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.css.CSSPropertyBuilder

class ActualModifier : Modifier {
    val styleHandlers = mutableListOf<CSSPropertyBuilder.() -> Unit>()
    val attrHandlers = mutableListOf<AttrsBuilder<*>.() -> Unit>()

    fun add(builder: CSSPropertyBuilder.() -> Unit) {
        styleHandlers.add(builder)
    }

    fun addAttributeBuilder(builder: AttrsBuilder<*>.() -> Unit) {
        attrHandlers.add(builder)
    }
}

fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
