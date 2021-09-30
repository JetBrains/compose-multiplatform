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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.FrameDispatcher
import java.awt.Component
import java.awt.event.MouseEvent
import kotlin.coroutines.CoroutineContext

/**
 * A virtual window for testing purposes.
 *
 * After [setContent] it composes content immediately and draws it on a virtual [surface].
 *
 * It doesn't dispatch frames by default. If frame dispatching is needed, pass appropriate
 * dispatcher as coroutineContext (for example, Dispatchers.Swing)
 */
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
        owners.render(canvas, nanoTime())
    }

    private val owners = DesktopOwners(
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
    val roots: Set<RootForTest> get() = owners.roots

    /**
     * Clear-up all acquired resources and stop all pending work
     */
    fun dispose() {
        owners.dispose()
        coroutineScope.cancel()
    }

    /**
     * Returns true if there are pending work scheduled by this window
     */
    fun hasInvalidations(): Boolean = owners.hasInvalidations()

    /**
     * Compose [content] immediately and draw it on a [surface]
     */
    fun setContent(content: @Composable () -> Unit) {
        owners.setContent(content = content)
        owners.render(canvas, nanoTime = nanoTime())
    }

    /**
     * Process mouse scroll event
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun onMouseScroll(x: Int, y: Int, event: MouseScrollEvent) {
        owners.onMouseScroll(x, y, event)
    }

    /**
     * Process mouse move event
     */
    fun onMouseMoved(x: Int, y: Int) {
        owners.onMouseMoved(
            x,
            y,
            MouseEvent(EventComponent, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 1, false)
        )
    }

    /**
     * Process mouse enter event
     */
    fun onMouseEntered(x: Int, y: Int) {
        owners.onMouseEntered(
            x,
            y,
            MouseEvent(EventComponent, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 1, false)
        )
    }

    /**
     * Process mouse exit event
     */
    fun onMouseExited() {
        owners.onMouseExited(
            -1,
            -1,
            MouseEvent(EventComponent, MouseEvent.MOUSE_MOVED, 0, 0, -1, -1, 1, false)
        )
    }

    companion object {
        private val EventComponent = object : Component() {}
    }
}