package co.touchlab.compose.darwin.internal

import org.jetbrains.compose.common.ui.Modifier
import platform.UIKit.UIView

class ActualModifier : Modifier {
    val modHandlers = mutableListOf<UIView.() -> Unit>()

    fun add(builder: UIView.() -> Unit) {
        modHandlers.add(builder)
    }
}

fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()