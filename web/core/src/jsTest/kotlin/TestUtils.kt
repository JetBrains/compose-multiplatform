package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.renderComposable
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlinx.dom.clear
import org.w3c.dom.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val testScope = MainScope()

class TestScope : CoroutineScope by testScope {

    val root = "div".asHtmlElement()
    init {
        document.body!!.appendChild(root)
    }

    fun composition(content: @Composable () -> Unit) {
        root.clear()
        renderComposable(root) {
            content()
        }
    }


    private val childrenIterator = root.children.asList().listIterator()
    fun nextChild() = childrenIterator.next() as HTMLElement
    fun currentChild() = root.children[childrenIterator.previousIndex()] as HTMLElement

    suspend fun waitChanges() {
        waitForChanges(root)
    }
}

internal fun runTest(block: suspend TestScope.() -> Unit): dynamic {
    val scope = TestScope()
    return scope.promise { block(scope) }
}

internal fun runBlockingTest(
    block: suspend CoroutineScope.() -> Unit
): dynamic = testScope.promise { this.block() }

internal fun String.asHtmlElement() = document.createElement(this) as HTMLElement

/* Currently, the recompositionRunner relies on AnimationFrame to run the recomposition and
applyChanges. Therefore we can use this method after updating the state and before making
assertions.

If tests get broken, then DefaultMonotonicFrameClock need to be checked if it still
uses window.requestAnimationFrame */
internal suspend fun waitForAnimationFrame() {
    suspendCoroutine<Unit> { continuation ->
        window.requestAnimationFrame {
            continuation.resume(Unit)
        }
    }
}

private object MutationObserverOptions : MutationObserverInit {
    override var childList: Boolean? = true
    override var attributes: Boolean? = true
    override var characterData: Boolean? = true
    override var subtree: Boolean? = true
    override var attributeOldValue: Boolean? = true
}

internal suspend fun waitForChanges(elementId: String) {
    waitForChanges(document.getElementById(elementId) as HTMLElement)
}

internal suspend fun waitForChanges(element: HTMLElement) {
    suspendCoroutine<Unit> { continuation ->
        val observer = MutationObserver { mutations, observer ->
            continuation.resume(Unit)
            observer.disconnect()
        }
        observer.observe(element, MutationObserverOptions)
    }
}