package com.jetbrains.compose.theme.intellij

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener

class ThemeChangeListener : LafManagerListener {
    override fun lookAndFeelChanged(source: LafManager) {
        SwingColorImpl.updateCurrentColors()
    }
}

