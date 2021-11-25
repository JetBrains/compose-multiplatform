package org.jetbrains.compose.common.internal

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import androidx.compose.ui.Modifier as JModifier
import org.jetbrains.compose.common.ui.Modifier

@Deprecated(message = webWidgetsDeprecationMessage)
private class ModifierElement : JModifier.Element

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
class ActualModifier : Modifier {
    var modifier: JModifier = ModifierElement()
}

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
