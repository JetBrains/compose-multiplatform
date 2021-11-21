package androidx.compose.ui.window

import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Catch exceptions during rendering frames, handling events, or processing background Compose operations.
 */
@ExperimentalComposeUiApi
fun interface WindowExceptionHandler {
    /**
     * Called synchronously in UI thread when an exception occurred  during rendering frames,
     * handling events, or processing background Compose operations.
     *
     * If the exception isn't fatal, you can ignore it, and the caller thread will continue to execute.
     * But usually you should throw the exception further after handling it:
     * ```
     * WindowExceptionHandler {
     *    log.writeException(it)
     *    throw it
     * }
     * ```
     */
    fun onException(throwable: Throwable)
}