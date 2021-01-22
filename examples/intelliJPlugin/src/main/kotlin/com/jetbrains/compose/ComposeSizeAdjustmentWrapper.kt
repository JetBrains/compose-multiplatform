package com.jetbrains.compose

import androidx.compose.desktop.ComposePanel
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import java.awt.Window
import javax.swing.JComponent

@Composable
fun ComposeSizeAdjustmentWrapper(
    window: DialogWrapper,
    panel: ComposePanel,
    preferredSize: IntSize,
    content: @Composable () -> Unit
) {
    var packed = false
    Box {
        content()
        Layout(
            content = {},
            modifier = Modifier.onGloballyPositioned { childCoordinates ->
                // adjust size of the dialog
                if (!packed) {
                    val contentSize = childCoordinates.parentCoordinates!!.size
                    panel.preferredSize = Dimension(
                        if (contentSize.width < preferredSize.width) preferredSize.width else contentSize.width,
                        if (contentSize.height < preferredSize.height) preferredSize.height else contentSize.height,
                    )
                    window.pack()
                    packed = true
                }
            },
            measureBlock = { _, _ ->
                layout(0, 0) {}
            }
        )
    }
}
