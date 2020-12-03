package org.jetbrains.compose.desktop.browser

import androidx.compose.desktop.AppWindowAmbient
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.globalPosition
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier

import org.jetbrains.skija.IRect
import org.jetbrains.skija.Bitmap
import org.jetbrains.skija.ImageInfo
import org.jetbrains.skija.ColorAlphaType

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
private val emptyBitmap: Bitmap
    get() {
        val bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo.makeS32(1, 1, ColorAlphaType.PREMUL))
        return bitmap
    }

@OptIn(
    ExperimentalFocus::class,
    ExperimentalFoundationApi::class
)
@Composable
fun CefView(browser: BrowserState) {
    val bitmap = remember { mutableStateOf(emptyBitmap) }
    val forceRecompose = remember { mutableStateOf(Any()) }
    val focusRequester = FocusRequester()

    if (browser.isReady()) {
        browser.onInvalidate {
            bitmap.value = browser.getBitmap()
            forceRecompose.value = Any()
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    width.value = placeable.width
                    height.value = placeable.height
                    browser.onLayout(x.value, y.value, width.value, height.value)

                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }
                .onGloballyPositioned { coordinates ->
                    x.value = coordinates.globalPosition.x.toInt()
                    y.value = coordinates.globalPosition.y.toInt()
                }
                .focusRequester(focusRequester)
                .focusObserver { browser.setFocused(it.isFocused) }
                .focus()
                .clickable(indication = null) { focusRequester.requestFocus() }
        ) {
            drawIntoCanvas { canvas ->
                forceRecompose.value
                bitmap.value
                canvas.nativeCanvas.drawBitmapRect(
                    bitmap.value,
                    IRect(0, 0, width.value, height.value).toRect()
                )
            }
        }
    }

    onDispose {
        browser.onDismiss()
    }
}
