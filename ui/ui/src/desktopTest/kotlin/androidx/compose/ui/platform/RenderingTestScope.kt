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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.yield
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Surface
import org.jetbrains.skiko.FrameDispatcher
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
internal fun renderingTest(
    width: Int,
    height: Int,
    platform: DesktopPlatform = DesktopPlatform.Linux,
    context: CoroutineContext = Dispatchers.Swing,
    block: suspend RenderingTestScope.() -> Unit
) = runBlocking(context) {
    val scope = RenderingTestScope(width, height, platform, context)
    try {
        scope.block()
    } finally {
        scope.dispose()
    }
}

internal class RenderingTestScope(
    private val width: Int,
    private val height: Int,
    private val platform: DesktopPlatform,
    coroutineContext: CoroutineContext
) {
    var currentTimeMillis = 0L

    private val coroutineScope = CoroutineScope(coroutineContext)
    private val frameDispatcher = FrameDispatcher(coroutineContext) {
        onRender(currentTimeMillis * 1_000_000)
    }

    val surface: Surface = Surface.makeRasterN32Premul(width, height)
    val canvas: Canvas = surface.canvas
    val owners = DesktopOwners(
        coroutineScope = coroutineScope,
        invalidate = frameDispatcher::scheduleFrame
    )
    private var owner: DesktopOwner? = null

    fun dispose() {
        owner?.dispose()
        frameDispatcher.cancel()
        coroutineScope.cancel()
    }

    private var onRender = CompletableDeferred<Unit>()

    fun setContent(content: @Composable () -> Unit) {
        owner?.dispose()
        val owner = DesktopOwner(owners)
        owner.setContent {
            CompositionLocalProvider(DesktopPlatformAmbient provides platform) {
                content()
            }
        }
        this.owner = owner
    }

    private fun onRender(timeNanos: Long) {
        canvas.clear(Color.Transparent.toArgb())
        owners.onFrame(canvas, width, height, timeNanos)
        onRender.complete(Unit)
    }

    suspend fun awaitNextRender() {
        onRender = CompletableDeferred()
        onRender.await()
    }

    suspend fun hasRenders(): Boolean {
        onRender = CompletableDeferred()
        // repeat multiple times because rendering can be dispatched on the next frames
        repeat(10) {
            yield()
        }
        return onRender.isCompleted
    }
}