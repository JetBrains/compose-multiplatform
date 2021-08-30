import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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