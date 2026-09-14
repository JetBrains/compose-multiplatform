@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.testutils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.browser.dom.Document
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.MutationObserver
import kotlinx.browser.dom.MutationObserverInit
import kotlinx.browser.dom.Window
import kotlinx.browser.dom.css.CSSStyleDeclaration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.dom.clear
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.JsAny
import kotlin.js.JsName
import kotlin.js.Promise
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@JsName("document")
private external val document: Document

@JsName("window")
private external val window: Window

internal expect fun renderTestComposable(
    root: HTMLElement,
    monotonicFrameClock: MonotonicFrameClock,
    content: @Composable () -> Unit,
)

internal expect fun createMutationObserverOptions(): MutationObserverInit

/**
 * This class provides a set of utils methods to simplify compose-web tests.
 * There is no need to create its instances manually.
 * @see [runTest]
 */
@ComposeWebExperimentalTestsApi
class TestScope : CoroutineScope by MainScope() {

    /**
     * It's used as a parent element for the composition.
     * It's added into the document's body automatically.
     */
    val root = document.createElement("div") as HTMLElement

    private var waitForRecompositionCompleteContinuation: Continuation<Unit>? = null
    private var nextChildIndex = 0

    init {
        document.body!!.appendChild(root)
    }

    private fun onRecompositionComplete() {
        waitForRecompositionCompleteContinuation?.resume(Unit)
        waitForRecompositionCompleteContinuation = null
    }

    /**
     * Cleans up the [root] content.
     * Creates a new composition with a given Composable [content].
     */
    @ComposeWebExperimentalTestsApi
    fun composition(content: @Composable () -> Unit) {
        root.clear()

        nextChildIndex = 0
        renderTestComposable(
            root = root,
            monotonicFrameClock = TestMonotonicClockImpl(
                onRecomposeComplete = this::onRecompositionComplete
            ),
            content = content,
        )
    }

    /**
     * @return a reference to the next child element of the root.
     * Subsequent calls will return next child reference every time.
     */
    fun nextChild() = nextChild<HTMLElement>()

    @Suppress("UNCHECKED_CAST")
    fun <T> nextChild() = root.children.item(nextChildIndex++) as T

    /**
     * Suspends until element with [elementId] observes any change to its html.
     */
    suspend fun waitForChanges(elementId: String) {
        waitForChanges(document.getElementById(elementId) as HTMLElement)
    }

    /**
     * Suspends until [element] observes any change to its html.
     */
    suspend fun waitForChanges(element: HTMLElement = root) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val observer = MutationObserver { _, observer ->
                continuation.resume(Unit)
                observer.disconnect()
            }
            observer.observe(element, createMutationObserverOptions())

            continuation.invokeOnCancellation {
                observer.disconnect()
            }
        }
    }

    /**
     * Suspends until recomposition completes.
     */
    suspend fun waitForRecompositionComplete() {
        suspendCancellableCoroutine<Unit> { continuation ->
            waitForRecompositionCompleteContinuation = continuation

            continuation.invokeOnCancellation {
                if (waitForRecompositionCompleteContinuation === continuation) {
                    waitForRecompositionCompleteContinuation = null
                }
            }
        }
    }
}

/**
 * Use this method to test compose-web components rendered using HTML.
 * Declare states and make assertions in [block].
 * Use [TestScope.composition] to define the code under test.
 *
 * For dynamic tests, use [TestScope.waitForRecompositionComplete]
 * after changing state's values and before making assertions.
 *
 * @see [TestScope.composition]
 * @see [TestScope.waitForRecompositionComplete]
 * @see [TestScope.waitForChanges].
 *
 * Test example:
 * ```
 * @Test
 * fun textChild() = runTest {
 *      var textState by mutableStateOf("inner text")
 *
 *      composition {
 *          Div {
 *              Text(textState)
 *          }
 *      }
 *      assertEquals("<div>inner text</div>", root.innerHTML)
 *
 *      textState = "new text"
 *      waitForRecompositionComplete()
 *
 *      assertEquals("<div>new text</div>", root.innerHTML)
 * }
 * ```
 */
@ComposeWebExperimentalTestsApi
fun runTest(block: suspend TestScope.() -> Unit): Promise<JsAny?> {
    val scope = TestScope()
    return scope.promise {
        block(scope)
        null
    }
}

@OptIn(ExperimentalTime::class)
private class TestMonotonicClockImpl(
    private val onRecomposeComplete: () -> Unit
) : MonotonicFrameClock {

    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R = suspendCoroutine { continuation ->
        window.requestAnimationFrame {
            val duration = it.toDuration(DurationUnit.MILLISECONDS)
            val result = onFrame(duration.inWholeNanoseconds)
            continuation.resume(result)
            onRecomposeComplete()
        }
    }
}

val HTMLElement.computedStyle: CSSStyleDeclaration
    get() = window.getComputedStyle(this)
