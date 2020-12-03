package org.jetbrains.compose.desktop.browser

import androidx.compose.desktop.AppManager
import androidx.compose.desktop.AppFrame
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import org.jetbrains.skija.Bitmap
import org.jetbrains.skiko.HardwareLayer
import javax.swing.JFrame
import org.cef.CefApp

class BrowserState {
    private val url = mutableStateOf("")
    private val isReady = mutableStateOf(false)
    private lateinit var browser: CefBrowserWrapper

    fun isReady(): Boolean {
        return isReady.value
    }

    fun loadURL(url: String) {
        if (!this::browser.isInitialized) {
            val frame = AppManager.focusedWindow
            if (frame != null) {
                onActive(frame, url)
            }
            return
        }
        isReady.value = false
        browser.loadURL(url)
        isReady.value = true
    }

    fun getBitmap(): Bitmap {
        return browser.getBitmap()
    }

    fun onLayout(x: Int, y: Int, width: Int, height: Int) {
        browser.onLayout(x, y, width, height)
    }

    fun onActive(frame: AppFrame, url: String) {
        val window = frame.window
        if (!window.isVisible()) {
            return
        }
        var layer = getHardwareLayer(window)
        if (layer == null) {
            throw Error("Browser initialization failed!")
        }
        browser = CefBrowserWrapper(
            startURL = url,
            layer = layer
        )
        browser.onActive()

        isReady.value = true
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
