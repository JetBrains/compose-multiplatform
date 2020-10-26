package org.jetbrains.compose.desktop.browser

import androidx.compose.desktop.AppManager
import androidx.compose.desktop.AppFrame
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.desktop.AppWindowAmbient
import org.jetbrains.skija.Bitmap
import org.jetbrains.skiko.HardwareLayer
import javax.swing.JFrame
import org.cef.CefApp

object BrowserState {
    public val url = mutableStateOf("https://www.google.com")

    private val isReady = mutableStateOf(false)
    private val frames = mutableStateOf(0)
    private lateinit var browser: ComposeBrowserWrapper

    fun isReady(): Boolean {
        return isReady.value
    }

    fun loadURL(url: String) {
        if (!isReady.value) {
            val app = AppManager.focusedWindow
            if (app != null) {
                init(app, url)
            }
        } else {
            isReady.value = false
            browser.loadURL(url)
            isReady.value = true
        }
    }

    fun init(app: AppFrame, url: String) {
        val window = app.window
        if (!window.isVisible()) {
            return
        }
        var layer = getHardwareLayer(window)
        if (layer == null) {
            throw Error("Browser initialization failed!")
        }
        browser = ComposeBrowserWrapper(
            startURL = url,
            layer = layer
        )
        isReady.value = true
    }

    fun getBitmap(): Bitmap {
        frames.value++
        return browser.getBitmap()
    }

    fun onLayout(x: Int, y: Int, width: Int, height: Int) {
        browser.onLayout(x, y, width, height)
    }

    fun onActive() {
        browser.onActive()
    }

    fun onDismiss() {
        browser.onDismiss()
    }

    fun setFocused(value: Boolean) {
        browser.setFocused(value)
    }

    fun onInvalidate(onInvalidate: (() -> Unit)?) {
        browser.onInvalidate = onInvalidate
    }

    private fun getHardwareLayer(window: JFrame): HardwareLayer? {
        val components = window.getContentPane().getComponents()
        for (component in components) {
            if (component is HardwareLayer) {
                return component
            }
        }
        return null
    }
}
