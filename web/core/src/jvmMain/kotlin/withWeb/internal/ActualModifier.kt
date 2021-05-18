package org.jetbrains.compose.common.internal

import androidx.compose.ui.Modifier as JModifier
import org.jetbrains.compose.common.ui.Modifier

private class ModifierElement : JModifier.Element

class ActualModifier : Modifier {
    var modifier: JModifier = ModifierElement()
}

fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
