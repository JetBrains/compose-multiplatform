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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import org.jetbrains.skija.Surface
import org.jetbrains.skiko.FrameDispatcher
import kotlin.coroutines.CoroutineContext

private val emptyDispatcher = object : CoroutineDispatcher() {
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
class TestComposeWindow(
    val width: Int,
    val height: Int,
    val density: Density = Density(1f, 1f),
    private val desktopPlatform: DesktopPlatform = DesktopPlatform.Linux,
    private val nanoTime: () -> Long = System::nanoTime,
    coroutineContext: CoroutineContext = emptyDispatcher
) {
    /**
     * Virtual surface on which the content will be drawn
     */
    val surface = Surface.makeRasterN32Premul(width, height)

    private val canvas = surface.canvas
    private var owner: DesktopOwner? = null

    private val coroutineScope = CoroutineScope(coroutineContext)
    private val frameDispatcher: FrameDispatcher = FrameDispatcher(
        onFrame = { onFrame() },
        context = coroutineScope.coroutineContext
    )

    private fun onFrame() {
        canvas.clear(Color.Transparent.toArgb())
        owners.onFrame(canvas, width, height, nanoTime())
    }

    private val owners = DesktopOwners(
        coroutineScope = coroutineScope,
        invalidate = frameDispatcher::scheduleFrame
    )

    /**
     * All currently registered [RootForTest]s
     */
    val roots: Set<DesktopRootForTest> get() = owners.list

    /**
     * Clear-up all acquired resources and stop all pending work
     */
    fun dispose() {
        owner?.dispose()
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
        check(owner == null) {
            "Cannot call setContent twice!"
        }

        val owner = DesktopOwner(owners, density)
        owner.setContent {
            CompositionLocalProvider(
                DesktopPlatformAmbient provides desktopPlatform
            ) {
                content()
            }
        }
        owner.setSize(width, height)
        owner.measureAndLayout()
        owner.draw(canvas)
        this.owner = owner
    }

    /**
     * Process mouse scroll event
     */
    fun onMouseScroll(x: Int, y: Int, event: MouseScrollEvent) {
        owners.onMouseScroll(x, y, event)
    }
}