package org.jetbrains.compose.desktop.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout
import androidx.compose.ui.layout.globalPosition
import androidx.compose.ui.onPositioned
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier

import org.jetbrains.skija.IRect
import org.jetbrains.skija.Bitmap

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

//EXPERIMENTAL FOCUS API
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.focus
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focusObserver
import androidx.compose.ui.focusRequester
import androidx.compose.foundation.clickable

private val width = mutableStateOf(0)
private val height = mutableStateOf(0)
private val x = mutableStateOf(0)
private val y = mutableStateOf(0)

@Composable
fun CefView() {
    CefLayout(BrowserState)
}

@Composable
fun CefLayout(browser: BrowserState) {

    var bitmap by mutableStateOf(browser.getBitmap())

    browser.onInvalidate(
        onInvalidate = {
            bitmap = browser.getBitmap()
    })
    
    CefCanvas(bitmap, browser)

    onActive {
        browser.onActive()
    }

    onDispose {
        browser.onDismiss()
    }
}

@Composable
@OptIn(
    ExperimentalFocus::class,
    ExperimentalFoundationApi::class
)
fun CefCanvas(bitmap: Bitmap, browser: BrowserState) {
    val focusRequester = FocusRequester()

    Canvas(
        modifier = Modifier
        .fillMaxSize()
        .onResized(browser)
        .onPositioned { coordinates ->
            x.value = coordinates.globalPosition.x.toInt()
            y.value = coordinates.globalPosition.y.toInt()
        }
        .focusRequester(focusRequester)
        .focusObserver { browser.setFocused(it.isFocused) }
        .focus()
        .clickable(indication = null) { focusRequester.requestFocus() }
    ) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmapRect(bitmap, IRect(0, 0, width.value, height.value).toRect())
        }
    }
}

private fun Modifier.onResized(browser: BrowserState) = Modifier.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    width.value = placeable.width
    height.value = placeable.height
    browser.onLayout(x.value, y.value, width.value, height.value)

    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}
