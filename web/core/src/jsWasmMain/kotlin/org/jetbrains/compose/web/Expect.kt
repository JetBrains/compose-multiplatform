package org.jetbrains.compose.web

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.DataTransfer
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
internal external interface EventTargetExtension {
    val checked: Boolean
    val valueAsNumber: Number?
    val value: String?
}


internal external interface CSSKeyframesRuleExtension {
    fun appendRule(cssRule: String)
}

internal external interface NativeEventExtension {
    val data: String?
    val dataTransfer: DataTransfer?
    val inputType: String?
    val isComposing: Boolean
    val locale: String

    val movementX: Int
    val movementY: Int
}
