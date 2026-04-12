package com.adrianwitaszak.ballast.web.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.BoxScope
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.silk.components.style.ComponentStyle
import com.varabyte.kobweb.silk.theme.SilkTheme
import com.varabyte.kobweb.silk.theme.colors.getColorMode
import org.jetbrains.compose.web.css.px

@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .backgroundColor(SilkTheme.palettes[getColorMode()].background)
            .borderRadius(14.px)
            .padding(16.px),
        content = content
    )
}

val CardStyle = ComponentStyle("cardstyle") {
    base {
        Modifier
            .backgroundColor(SilkTheme.palettes[colorMode].background)
            .borderRadius(14.px)
            .padding(16.px)
    }
}
