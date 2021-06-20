package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import co.touchlab.compose.darwin.UIKitApplier
import co.touchlab.compose.darwin.UIViewWrapper
import co.touchlab.compose.darwin.internal.castOrCreate
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.core.graphics.toUIColor
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.TextUnit
import platform.UIKit.UILabel

@Composable
actual fun TextActual(
    text: String,
    modifier: Modifier,
    color: Color,
    size: TextUnit
) {
    println("Text showing $text")
    ComposeNode<UIViewWrapper<UILabel>, UIKitApplier>(
        factory = { UIViewWrapper(UILabel()) },
        update = {
            set(text) { v -> view.text = v }
            set(color) { v -> view.textColor = v.toUIColor() }
            set(size) { v -> view.font = view.font.fontWithSize(size.value.toDouble()) }
            set(modifier) { v ->
                v.castOrCreate().modHandlers.forEach { block -> block.invoke(view) }
            }
        },
    )
}