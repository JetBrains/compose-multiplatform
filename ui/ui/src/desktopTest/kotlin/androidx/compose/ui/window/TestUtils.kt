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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import java.awt.GraphicsEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assume.assumeFalse

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
            val exceptionHandler = TestExceptionHandler()
            withExceptionHandler(exceptionHandler) {
                val scope = WindowTestScope(this, useDelay, exceptionHandler)
                scope.body()
                scope.exitTestApplication()
            }
            exceptionHandler.throwIfCaught()
        }
    }
}

private inline fun withExceptionHandler(
    handler: Thread.UncaughtExceptionHandler,
    body: () -> Unit
) {
    val old = Thread.currentThread().uncaughtExceptionHandler
    Thread.currentThread().uncaughtExceptionHandler = handler
    try {
        body()
    } finally {
        Thread.currentThread().uncaughtExceptionHandler = old
    }
}

internal class TestExceptionHandler : Thread.UncaughtExceptionHandler {
    private var exception: Throwable? = null

    fun throwIfCaught() {
        exception?.also {
            throw it
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (exception != null) {
            exception?.addSuppressed(throwable)
        } else {
            exception = throwable
        }
    }
}

internal class WindowTestScope(
    private val scope: CoroutineScope,
    private val useDelay: Boolean,
    private val exceptionHandler: TestExceptionHandler
) : CoroutineScope by CoroutineScope(scope.coroutineContext + Job()) {
    var isOpen by mutableStateOf(true)
    private val initialRecomposers = Recomposer.runningRecomposers.value

    // TODO(demin) replace launchApplication to launchTestApplication in all tests,
    //  because we don't close the window with simple launchApplication
    fun launchTestApplication(
        content: @Composable ApplicationScope.() -> Unit
    ) = launchApplication {
        if (isOpen) {
            content()
        }
    }

    // TODO(demin) remove when we migrate from launchApplication to launchTestApplication (see TODO above)
    fun exitApplication() {
        isOpen = false
    }

    fun exitTestApplication() {
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

        Snapshot.sendApplyNotifications()
        for (recomposerInfo in Recomposer.runningRecomposers.value - initialRecomposers) {
            recomposerInfo.state.takeWhile { it > Recomposer.State.Idle }.collect()
        }

        exceptionHandler.throwIfCaught()
    }
}