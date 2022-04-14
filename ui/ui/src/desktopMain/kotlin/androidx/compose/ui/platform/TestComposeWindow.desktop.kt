/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION") // https://github.com/JetBrains/compose-jb/issues/1514

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.FrameDispatcher
import java.awt.Component
import java.awt.event.MouseWheelEvent
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@PublishedApi
internal val EmptyDispatcher = object : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = Unit
}

/**
 * A virtual window for testing purposes.
 *
 * After [setContent] it composes content immediately and draws it on a virtual [surface].
 *
 * It doesn't dispatch frames by default. If frame dispatching is needed, pass appropriate
 * dispatcher as coroutineContext (for example, Dispatchers.Swing)
 */
@OptIn(ExperimentalComposeUiApi::class)
class TestComposeWindow(
    val width: Int,
    val height: Int,
    val density: Density = Density(1f, 1f),
    private val nanoTime: () -> Long = System::nanoTime,
    coroutineContext: CoroutineContext = EmptyDispatcher
) {
    /**
     * Virtual surface on which the content will be drawn
     */
    val surface = Surface.makeRasterN32Premul(width, height)

    private val canvas = surface.canvas

    private val coroutineScope = CoroutineScope(coroutineContext + Job())
    private val frameDispatcher: FrameDispatcher = FrameDispatcher(
        onFrame = { onFrame() },
        context = coroutineScope.coroutineContext
    )

    private fun onFrame() {
        canvas.clear(Color.Transparent.toArgb())
        scene.flushEffects()
        scene.render(canvas, nanoTime())
    }

    internal val component = DummyPlatformComponent
    private val scene = ComposeScene(
        coroutineScope.coroutineContext,
        component,
        density,
        invalidate = frameDispatcher::scheduleFrame
    ).apply {
        constraints = Constraints(maxWidth = width, maxHeight = height)
    }

    val currentCursor
        get() = component.componentCursor

    /**
     * All currently registered [RootForTest]s
     */
    @OptIn(InternalComposeUiApi::class)
    val roots: Set<RootForTest> get() = scene.roots

    /**
     * Clear-up all acquired resources and stop all pending work
     */
    fun dispose() {
        scene.close()
        coroutineScope.cancel()
    }

    /**
     * Returns true if there are pending work scheduled by this window
     */
    fun hasInvalidations(): Boolean = scene.hasInvalidations()

    /**
     * Compose [content] immediately and draw it on a [surface]
     */
    fun setContent(content: @Composable () -> Unit) {
        scene.constraints = Constraints(maxWidth = width, maxHeight = height)
        scene.setContent(content = content)
        scene.flushEffects()
        scene.render(canvas, nanoTime = nanoTime())
    }

    fun render() {
        scene.render(canvas, nanoTime = nanoTime())
    }

    /**
     * Process mouse scroll event
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun onMouseScroll(x: Int, y: Int, event: MouseScrollEvent) {
        val delta = when (event.delta) {
            is MouseScrollUnit.Line -> event.delta.value
            is MouseScrollUnit.Page -> event.delta.value
        }
        val wheelRotation = sign(delta)
        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset(x.toFloat(), y.toFloat()),
            scrollDelta = if (event.orientation == MouseScrollOrientation.Vertical) {
                Offset(0f, wheelRotation)
            } else {
                Offset(wheelRotation, 0f)
            },
            nativeEvent = MouseWheelEvent(
                EventComponent,
                MouseWheelEvent.MOUSE_WHEEL,
                0,
                0,
                0,
                0,
                0,
                false,
                if (event.delta is MouseScrollUnit.Line) {
                    MouseWheelEvent.WHEEL_UNIT_SCROLL
                } else {
                    MouseWheelEvent.WHEEL_BLOCK_SCROLL
                },
                abs(delta.roundToInt()),
                wheelRotation.roundToInt()
            )
        )
    }

    /**
     * Process mouse move event
     */
    fun onMouseMoved(x: Int, y: Int) {
        scene.sendPointerEvent(
            eventType = PointerEventType.Move,
            position = Offset(x.toFloat(), y.toFloat())
        )
    }

    /**
     * Process mouse enter event
     */
    fun onMouseEntered(x: Int, y: Int) {
        scene.sendPointerEvent(
            eventType = PointerEventType.Enter,
            position = Offset(x.toFloat(), y.toFloat())
        )
    }

    /**
     * Process mouse exit event
     */
    fun onMouseExited() {
        scene.sendPointerEvent(
            eventType = PointerEventType.Exit,
            position = Offset(-1f, -1f)
        )
    }

    companion object {
        private val EventComponent = object : Component() {}
    }
}