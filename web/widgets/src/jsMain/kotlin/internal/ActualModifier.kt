package org.jetbrains.compose.common.internal

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.css.StyleBuilder
import org.jetbrains.compose.web.attributes.AttrsBuilder

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
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
@Deprecated(message = webWidgetsDeprecationMessage)
fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
