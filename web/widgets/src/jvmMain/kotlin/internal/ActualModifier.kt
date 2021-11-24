package org.jetbrains.compose.common.internal

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import androidx.compose.ui.Modifier as JModifier
import org.jetbrains.compose.common.ui.Modifier

@Deprecated(message = "compose.web.web-widgets API is deprecated")
private class ModifierElement : JModifier.Element

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
class ActualModifier : Modifier {
    var modifier: JModifier = ModifierElement()
}

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
