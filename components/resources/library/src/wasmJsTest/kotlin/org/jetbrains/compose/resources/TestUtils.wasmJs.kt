package org.jetbrains.compose.resources

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest

@JsFun("() => ''")
private external fun jsRef(): JsAny


actual typealias TestReturnType = Any
/**
 * Runs the [block] in a coroutine.
 */
actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit): TestReturnType = MainScope().promise {
    block()
    jsRef()
}