/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.native

import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.pointer.toCompose
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.getTimeMilliseconds
import androidx.compose.ui.input.pointer.PointerEventType
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoView
import org.jetbrains.skiko.SkikoInputEvent
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.SkikoGestureEvent
import org.jetbrains.skiko.SkikoPointerEventKind
import org.jetbrains.skiko.SkikoGestureEventKind

internal class ComposeLayer {
    private var isDisposed = false

    internal val layer = SkiaLayer()

    inner class ComponentImpl : SkikoView, PlatformComponent {
        override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
            val contentScale = layer.contentScale
            canvas.scale(contentScale, contentScale)
            scene.render(canvas/*, (width / contentScale).toInt(), (height / contentScale).toInt()*/, nanoTime)
        }

        override fun onInputEvent(event: SkikoInputEvent) {
            TODO("need scene.sendInputEvent")
        }

        override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
            TODO("need scene.sendKeyEvent")
        }

        override fun onGestureEvent(event: SkikoGestureEvent) {
            when (event.kind) {
                SkikoGestureEventKind.TAP -> {
                    // uikit doesn't give us a TAP press, so we send the Press event ourselves.
                    scene.sendPointerEvent(
                        eventType = PointerEventType.Press,
                        // TODO: account for the proper density.
                        position = Offset(event.x.toFloat(), event.y.toFloat()), // * density,
                        timeMillis = getTimeMilliseconds(),
                        type = PointerType.Touch,
                        nativeEvent = event
                    )
                    scene.sendPointerEvent(
                        eventType = event.state.toCompose(),
                        // TODO: account for the proper density.
                        position = Offset(event.x.toFloat(), event.y.toFloat()), // * density,
                        timeMillis = getTimeMilliseconds(),
                        type = PointerType.Touch,
                        nativeEvent = event
                    )
                }
                SkikoGestureEventKind.LONGPRESS ->
                    scene.sendPointerEvent(
                        eventType = event.state.toCompose(),
                        // TODO: account for the proper density.
                        position = Offset(event.x.toFloat(), event.y.toFloat()), // * density,
                        timeMillis = getTimeMilliseconds(),
                        type = PointerType.Touch,
                        nativeEvent = event
                    )
            }
        }

        override fun onPointerEvent(event: SkikoPointerEvent) {
            scene.sendPointerEvent(
                eventType = event.kind.toCompose(),
                // TODO: account for the proper density.
                position = Offset(event.x.toFloat(), event.y.toFloat()), // * density,
                timeMillis = getTimeMilliseconds(),
                type = PointerType.Mouse,
                nativeEvent = event
            )
        }
    }

    val view = ComponentImpl()

    init {
        layer.skikoView = view
    }

    private val scene = ComposeScene(
        getMainDispatcher(),
        view,
        Density(1f),
        layer::needRedraw
    )

    fun dispose() {
        check(!isDisposed)
        this.layer.detach()
        scene.dispose()
        _initContent = null
        isDisposed = true
    }

    fun setContent(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            scene.setContent(
                onPreviewKeyEvent = onPreviewKeyEvent,
                onKeyEvent = onKeyEvent,
                content = content
            )
        }
        initContent()
    }

    private var _initContent: (() -> Unit)? = null

    private fun initContent() {
        // TODO: do we need isDisplayable on SkiaLyer?
        // if (layer.isDisplayable) {
            _initContent?.invoke()
            _initContent = null
        // }
    }
}

internal expect fun getMainDispatcher(): CoroutineDispatcher

