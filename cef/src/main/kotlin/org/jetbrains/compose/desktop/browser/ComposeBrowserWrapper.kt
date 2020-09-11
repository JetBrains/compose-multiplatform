package org.jetbrains.compose.desktop.browser

import androidx.compose.ui.unit.IntOffset

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionAdapter
import java.awt.KeyboardFocusManager

import java.nio.ByteBuffer

import org.cef.CefApp
import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.BrowserView
import org.cef.handler.CefFocusHandlerAdapter

import org.jetbrains.skija.Bitmap
import org.jetbrains.skiko.HardwareLayer

open class ComposeBrowserWrapper {
    private var offset = IntOffset(0, 0)
    private var isFocused = false
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

        layer.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                if (isInLayer(event))
                    browser.onMouseEvent(event)
            }
            override fun mouseReleased(event: MouseEvent) {
                if (isInLayer(event))
                    browser.onMouseEvent(event)
            }
        })

        layer.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(event: MouseEvent) {
                if (isInLayer(event))
                    browser.onMouseEvent(event)
            }
            override fun mouseDragged(event: MouseEvent) {
                if (isInLayer(event))
                    browser.onMouseEvent(event)
            }
        })

        layer.addMouseWheelListener(object : MouseWheelListener {
            override fun mouseWheelMoved(event: MouseWheelEvent) {
                if (isInLayer(event))
                    browser.onMouseScrollEvent(event)
            }
        })
    
        layer.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                if (!isFocused) {
                    return
                }
                browser.onKeyEvent(event)
            }
            override fun keyReleased(event: KeyEvent) {
                if (!isFocused) {
                    return
                }
                browser.onKeyEvent(event)
            }
            override fun keyTyped(event: KeyEvent) {
                if (!isFocused) {
                    return
                }
                browser.onKeyEvent(event)
            }
        })
    }

    private fun isInLayer(event: MouseEvent): Boolean {
        val x = event.x
        val y = event.y
        if (x > offset.x && y > offset.y) {
            return true
        }
        return false
    }

    fun loadURL(url: String) {
        browser.loadURL(url)
    }

    fun setFocused(value: Boolean) {
        isFocused = value
    }

    fun getBitmap(): Bitmap {
        return browser.getBitmap()
    }

    fun onLayout(x: Int, y: Int, width: Int, height: Int) {
        offset = IntOffset(x, y)
        browser.onResized(x, y, width, height)
    }

    fun onActive() {
        browser.onStart()
    }

    fun onDismiss() {
        CefApp.getInstance().dispose()
    }
}