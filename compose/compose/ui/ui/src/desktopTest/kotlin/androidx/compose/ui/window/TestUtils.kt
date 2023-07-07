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
import androidx.compose.ui.awaitEDT
import java.awt.GraphicsEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.skiko.MainUIDispatcher
import org.junit.Assume.assumeFalse
import androidx.compose.ui.window.launchApplication as realLaunchApplication


internal fun runApplicationTest(
    /**
     * Use delay additionally to `yield` in `await*` functions
     *
     * Set this property only if you sure that you can't easily make the test deterministic
     * (non-flaky).
     *
     * We have to use `useDelay` in some Linux Tests, because Linux can behave in
     * non-deterministic way when we change position/size very fast (see the snippet below).
     */
    useDelay: Boolean = false,
    delayMillis: Long = 500,
    // TODO ui-test solved this issue by passing InfiniteAnimationPolicy to CoroutineContext. Do the same way here
    /**
     * Hint for `awaitIdle` that the content contains animations (ProgressBar, TextField cursor, etc).
     * In this case, we use `delay` instead of waiting for state changes to end.
     */
    hasAnimations: Boolean = false,
    animationsDelayMillis: Long = 500,
    timeoutMillis: Long = 30000,
    body: suspend WindowTestScope.() -> Unit
) {
    assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

    runBlocking(MainUIDispatcher) {
        withTimeout(timeoutMillis) {
            val exceptionHandler = TestExceptionHandler()
            withExceptionHandler(exceptionHandler) {
                val scope = WindowTestScope(
                    scope = this,
                    delayMillis = if (useDelay) delayMillis else -1,
                    animationsDelayMillis = if (hasAnimations) animationsDelayMillis else -1,
                    exceptionHandler = exceptionHandler)
                try {
                    scope.body()
                } finally {
                    scope.exitTestApplication()
                }
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
    private val delayMillis: Long,
    private val animationsDelayMillis: Long,
    private val exceptionHandler: TestExceptionHandler
) : CoroutineScope by CoroutineScope(scope.coroutineContext + Job()) {
    var isOpen by mutableStateOf(true)
    private val initialRecomposers = Recomposer.runningRecomposers.value

    fun launchTestApplication(
        content: @Composable ApplicationScope.() -> Unit
    ) = realLaunchApplication {
        if (isOpen) {
            content()
        }
    }

    // Overload `launchApplication` to prohibit calling it from tests
    @Deprecated(
        "Do not use `launchApplication` from tests; use `launchTestApplication` instead",
        level = DeprecationLevel.ERROR
    )
    fun launchApplication(
        @Suppress("UNUSED_PARAMETER") content: @Composable ApplicationScope.() -> Unit
    ): Nothing {
        error("Do not use `launchApplication` from tests; use `launchTestApplication` instead")
    }

    suspend fun exitTestApplication() {
        isOpen = false
        awaitIdle()  // Wait for the windows to actually complete disposing
    }

    suspend fun awaitIdle() {
        if (delayMillis >= 0) {
            delay(delayMillis)
        }

        awaitEDT()

        Snapshot.sendApplyNotifications()

        if (animationsDelayMillis >= 0) {
            delay(animationsDelayMillis)
        } else {
            for (recomposerInfo in Recomposer.runningRecomposers.value - initialRecomposers) {
                recomposerInfo.state.takeWhile { it > Recomposer.State.Idle }.collect()
            }
        }

        exceptionHandler.throwIfCaught()
    }
}
