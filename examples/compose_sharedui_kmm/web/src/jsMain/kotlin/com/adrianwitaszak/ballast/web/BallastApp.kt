package com.adrianwitaszak.ballast.web

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.minHeight
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.silk.SilkApp
import com.varabyte.kobweb.silk.components.layout.Surface
import com.varabyte.kobweb.silk.components.style.common.SmoothColorStyle
import com.varabyte.kobweb.silk.components.style.toModifier
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.init.registerBaseStyle
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import kotlinx.browser.localStorage
import org.jetbrains.compose.web.css.vh

private const val COLOR_MODE_KEY = "admin:colorMode"

@InitSilk
fun updateTheme(ctx: InitSilkContext) = ctx.config.apply {
    initialColorMode = localStorage.getItem(COLOR_MODE_KEY)?.let { ColorMode.valueOf(it) } ?: ColorMode.LIGHT
}

@InitSilk
fun registerGlobalStyles(ctx: InitSilkContext) = ctx.config.apply {
    ctx.stylesheet.registerBaseStyle("body") {
        Modifier.fontFamily(
            "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "Oxygen", "Ubuntu",
            "Cantarell", "Fira Sans", "Droid Sans", "Helvetica Neue", "sans-serif"
        )
    }
}

@App
@Composable
fun BallastApp(content: @Composable () -> Unit) {
    SilkApp {
        Surface(SmoothColorStyle.toModifier().minHeight(100.vh)) {
            content()
        }
    }
}
