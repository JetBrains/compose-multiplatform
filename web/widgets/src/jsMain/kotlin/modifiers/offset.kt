package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.px

@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
actual fun Modifier.offset(x: Dp, y: Dp): Modifier = castOrCreate().apply {
    add {
        marginLeft(x.value.px)
        marginTop(y.value.px)
    }
}
