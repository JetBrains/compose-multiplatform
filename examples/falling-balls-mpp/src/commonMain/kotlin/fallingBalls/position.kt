package androidx.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unitimplementation
import androidx.compose.foundation.layout.offset
//import androidx.compose.ui.Modifier as JModifier

@Composable
fun Modifier.position(width: Dp, height: Dp): Modifier = // castOrCreate().apply {
    /*modifier = modifier.*/offset(width, height)
//}
/*
class ActualModifier : Modifier {
    var modifier: JModifier = ModifierElement()
    fun all(predicate: (Modifier.Element) -> Boolean): Boolean = error("fixme")
}

fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
*/
