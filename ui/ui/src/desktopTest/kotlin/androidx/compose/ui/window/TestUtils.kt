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

package androidx.compose.ui.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assume.assumeFalse
import java.awt.GraphicsEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runApplicationTest(
    /**
     * Use delay(500) additionally to `yield` in `await*` functions
     *
     * Set this property only if you sure that you can't easily make the test deterministic
     * (non-flaky).
     *
     * We have to use `useDelay` in some Linux Tests, because Linux can behave in
     * non-deterministic way when we change position/size very fast (see the snippet below)
     */
    useDelay: Boolean = false,
    body: suspend WindowTestScope.() -> Unit
) {
    assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

    runBlocking(Dispatchers.Swing) {
        withTimeout(30000) {
            val testScope = WindowTestScope(this, useDelay)
            if (testScope.isOpen) {
                testScope.body()
            }
        }
    }
}

/* Snippet that demonstrated the issue with window state listening on Linux

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.yield
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JFrame

fun main()  {
    runBlocking(Dispatchers.Swing) {
        repeat(10) {
            val actions = mutableListOf<String>()
            val frame = JFrame()
            frame.addComponentListener(object : ComponentAdapter() {
                override fun componentMoved(e: ComponentEvent?) {
                    actions.add(frame.x.toString())
                }
            })
            frame.location = Point(200, 200)
            frame.isVisible = true
            yield()
//                delay(200)
            actions.add("set300")
            frame.location = Point(300, 300)
            delay(200)
            /**
             * output is [200, set300, 300, 200, 300] on Linux
             * (see 200, 300 at the end, they are unexpected events that make impossible to write
             * robust tests without delays)
             */
            println(actions)
            frame.dispose()
        }
    }
}
*/

internal class WindowTestScope(
    private val scope: CoroutineScope,
    private val useDelay: Boolean
) : CoroutineScope by CoroutineScope(scope.coroutineContext + Job()) {
    var isOpen by mutableStateOf(true)

    fun exitApplication() {
        isOpen = false
    }

    suspend fun awaitIdle() {
        if (useDelay) {
            delay(500)
        }
        // TODO(demin): It seems this not-so-good synchronization
        //  doesn't cause flakiness in our window tests.
        //  But more robust solution will be to use something like
        //  TestCoroutineDispatcher/FlushCoroutineDispatcher (but we can't use it in a pure form,
        //  because there are Swing/system events that we don't control).
        // Most of the work usually is done after the first yield(), almost all of the work -
        // after fourth yield()
        repeat(100) {
            yield()
        }
    }
}

private val os = System.getProperty("os.name").lowercase()
internal val isLinux = os.startsWith("linux")
internal val isWindows = os.startsWith("win")
internal val isMacOs = os.startsWith("mac")