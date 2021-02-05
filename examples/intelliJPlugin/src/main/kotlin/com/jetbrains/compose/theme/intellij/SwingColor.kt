package com.jetbrains.compose.theme.intellij

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import javax.swing.UIManager
import java.awt.Color as AWTColor

interface SwingColor {
    companion object : SwingColor by SwingColorImpl

    val background: Color
    val onBackground: Color
}

object SwingColorImpl : SwingColor {
    private val _backgroundState: MutableState<Color> = mutableStateOf(getBackground)
    private val _onBackgroundState: MutableState<Color> = mutableStateOf(getOnBackground)

    override val background: Color get() = _backgroundState.value
    override val onBackground: Color get() = _onBackgroundState.value

    private const val BACKGROUND_KEY = "Panel.background"
    private const val ON_BACKGROUND_KEY = "Panel.foreground"

    private val getBackground get() = UIManager.getColor(BACKGROUND_KEY).asComposeColor
    private val getOnBackground get() = UIManager.getColor(ON_BACKGROUND_KEY).asComposeColor

    internal fun updateCurrentColors() {
        _backgroundState.value = getBackground
        _onBackgroundState.value = getOnBackground
    }

    private val AWTColor.asComposeColor: Color get() = Color(red, green, blue, alpha)
}