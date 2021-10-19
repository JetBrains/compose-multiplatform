package org.jetbrains.compose.desktop.browser

import androidx.compose.ui.unit.IntOffset
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.KeyboardFocusManager
import java.nio.ByteBuffer
import org.cef.CefApp
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.BrowserView
import org.cef.handler.CefFocusHandlerAdapter
import org.jetbrains.skija.Bitmap
import org.jetbrains.skija.ImageInfo
import org.jetbrains.skija.ColorAlphaType
import org.jetbrains.skiko.HardwareLayer

class CefBrowserWrapper {
    private var cefFocus = true
    private val browser: BrowserView
    public var onInvalidate: (() -> Unit)? = null

    constructor(layer: HardwareLayer, startURL: String) {
        if (!CefApp.startup(arrayOf(""))) {
            throw Error("CEF initialization failed!")
        }
        val settings = CefSettings()
        settings.windowless_rendering_enabled = true
        val cefApp = CefApp.getInstance(settings)
        val client = cefApp.createClient()
        
        browser = object : BrowserView(layer, client, startURL, null) {
            public override fun onBitmapChanged(popup: Boolean, buffer: ByteBuffer, width: Int, height: Int) {
                super.onBitmapChanged(popup, buffer, width, height)
                onInvalidate?.invoke()
            }
        }

        client.addFocusHandler(object : CefFocusHandlerAdapter() {
            public override fun onGotFocus(cefBrowser: CefBrowser) {
                if (cefFocus) {
                    return
                }
                cefFocus = true
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner()
                browser.onFocusGained()
            }

            public override fun onTakeFocus(cefBrowser: CefBrowser, next: Boolean) {
                cefFocus = false
                browser.onFocusLost()
            }
        })
    }

    fun loadURL(url: String) {
        browser.loadURL(url)
    }

    fun getBitmap(): Bitmap {
        return browser.getBitmap()
    }

    fun onLayout(x: Int, y: Int, width: Int, height: Int) {
        browser.onResized(x, y, width, height)
    }

    fun onActive() {
        browser.onStart()
    }

    fun onDismiss() {
        CefApp.getInstance().dispose()
    }

    fun onMouseEvent(event: MouseEvent) {
        browser.onMouseEvent(event)
    }

    fun onMouseScrollEvent(event: MouseWheelEvent) {
        browser.onMouseScrollEvent(event)
    }

    fun onKeyEvent(event: KeyEvent) {
        if (cefFocus) {
            browser.onKeyEvent(event)
        }
    }
}

internal val emptyBitmap: Bitmap
    get() {
        val bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo.makeS32(1, 1, ColorAlphaType.PREMUL))
        return bitmap
    }