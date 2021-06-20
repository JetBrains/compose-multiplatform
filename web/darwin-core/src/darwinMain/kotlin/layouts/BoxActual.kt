package org.jetbrains.compose.common.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import co.touchlab.compose.darwin.UIKitApplier
import co.touchlab.compose.darwin.UIViewWrapper
import co.touchlab.compose.darwin.internal.castOrCreate
import org.jetbrains.compose.common.ui.Modifier
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.addSubview
import platform.UIKit.sizeToFit

@Composable
internal actual fun BoxActual(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    ComposeNode<UIViewWrapper<UIView>, UIKitApplier>(
        factory = { val view = UIView()
//            view.sizeToFit()
            val label = UILabel()
            label.text = "filler label insides"
            view.addSubview(label)
            UIViewWrapper(view) },
        update = {
            set(modifier) { v ->
                v.castOrCreate().modHandlers.forEach { block -> block.invoke(view) }
            }
        },
        content = content
    )
}