package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import co.touchlab.compose.darwin.UIControlWrapper
import co.touchlab.compose.darwin.UIKitApplier
import org.jetbrains.compose.common.ui.Modifier
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlStateNormal

@Composable
actual fun ButtonActual(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ComposeNode<UIControlWrapper<UIButton>, UIKitApplier>(
        factory = { UIControlWrapper(UIButton().apply {
            setTitleColor(UIColor.blackColor, UIControlStateNormal)
            setTitle("Hmm button", UIControlStateNormal)
        }) },
        update = {
            set(onClick) { oc -> updateOnClick(onClick) }
        },
    )
}