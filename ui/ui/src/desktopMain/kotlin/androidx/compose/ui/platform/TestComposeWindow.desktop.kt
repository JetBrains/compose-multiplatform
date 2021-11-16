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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.FrameDispatcher
import java.awt.Component
import kotlin.coroutines.CoroutineContext

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
        scene.render(canvas, nanoTime())
    }

    private val scene = ComposeScene(
        coroutineScope.coroutineContext,
        density,
        invalidate = frameDispatcher::scheduleFrame
    ).apply {
        constraints = Constraints(maxWidth = width, maxHeight = height)
    }

    /**
     * All currently registered [RootForTest]s
     */
    @OptIn(InternalComposeUiApi::class)
    val roots: Set<RootForTest> get() = scene.roots

    /**
     * Clear-up all acquired resources and stop all pending work
     */
    fun dispose() {
        scene.dispose()
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
        scene.sendPointerScrollEvent(
            position = Offset(x.toFloat(), y.toFloat()),
            delta = event.delta,
            orientation = event.orientation
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