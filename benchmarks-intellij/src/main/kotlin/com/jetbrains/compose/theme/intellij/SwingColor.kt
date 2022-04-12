package com.jetbrains.compose.theme.intellij

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import javax.swing.UIManager
import java.awt.Color as AWTColor

interface SwingColor {
    val background: Color
    val onBackground: Color
}

@Composable
fun SwingColor(): SwingColor {
    val swingColor = remember { SwingColorImpl() }

    val messageBus = remember {
        ApplicationManager.getApplication().messageBus.connect()
    }

    remember(messageBus) {
        messageBus.subscribe(
            LafManagerListener.TOPIC,
            ThemeChangeListener(swingColor::updateCurrentColors)
        )
    }

    DisposableEffect(messageBus) {
        onDispose {
            messageBus.disconnect()
        }
    }

    return swingColor
}

private class SwingColorImpl : SwingColor {
    private val _backgroundState: MutableState<Color> = mutableStateOf(getBackgroundColor)
    private val _onBackgroundState: MutableState<Color> = mutableStateOf(getOnBackgroundColor)

    override val background: Color get() = _backgroundState.value
    override val onBackground: Color get() = _onBackgroundState.value

    private val getBackgroundColor get() = getColor(BACKGROUND_KEY)
    private val getOnBackgroundColor get() = getColor(ON_BACKGROUND_KEY)

    fun updateCurrentColors() {
        _backgroundState.value = getBackgroundColor
        _onBackgroundState.value = getOnBackgroundColor
    }

    private val AWTColor.asComposeColor: Color get() = Color(red, green, blue, alpha)
    private fun getColor(key: String): Color = UIManager.getColor(key).asComposeColor

    companion object {
        private const val BACKGROUND_KEY = "Panel.background"
        private const val ON_BACKGROUND_KEY = "Panel.foreground"
    }
}