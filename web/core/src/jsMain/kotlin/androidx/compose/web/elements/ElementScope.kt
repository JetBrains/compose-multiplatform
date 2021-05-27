package androidx.compose.web.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

interface DOMScope<out TElement : Element>

interface ElementScope<out TElement : Element> : DOMScope<TElement> {

    @Composable
    @NonRestartableComposable
    fun DisposableRefEffect(
        key: Any?,
        effect: DisposableEffectScope.(TElement) -> DisposableEffectResult
    )

    @Composable
    @NonRestartableComposable
    fun DisposableRefEffect(
        effect: DisposableEffectScope.(TElement) -> DisposableEffectResult
    ) {
        DisposableRefEffect(null, effect)
    }

    @Composable
    @NonRestartableComposable
    fun DomSideEffect(key: Any?, effect: DomEffectScope.(TElement) -> Unit)

    @Composable
    @NonRestartableComposable
    fun DomSideEffect(effect: DomEffectScope.(TElement) -> Unit)
}

abstract class ElementScopeBase<out TElement : Element> : ElementScope<TElement> {
    abstract val element: TElement

    private var nextDisposableDomEffectKey = 0

    @Composable
    @NonRestartableComposable
    override fun DisposableRefEffect(
        key: Any?,
        effect: DisposableEffectScope.(TElement) -> DisposableEffectResult
    ) {
        DisposableEffect(key) { effect(element) }
    }

    @Composable
    @NonRestartableComposable
    @OptIn(ComposeCompilerApi::class)
    override fun DomSideEffect(
        key: Any?,
        effect: DomEffectScope.(TElement) -> Unit
    ) {
        val changed = currentComposer.changed(key)
        val effectHolder = remember(key) {
            DomDisposableEffectHolder(this)
        }
        SideEffect {
            if (changed) effectHolder.effect(element)
        }
    }

    @Composable
    @NonRestartableComposable
    override fun DomSideEffect(effect: DomEffectScope.(TElement) -> Unit) {
        DomSideEffect(nextDisposableDomEffectKey++, effect)
    }
}

open class ElementScopeImpl<TElement : Element> : ElementScopeBase<TElement>() {
    public override lateinit var element: TElement
}

interface DomEffectScope {
    fun onDispose(disposeEffect: (Element) -> Unit)
}

private class DomDisposableEffectHolder(
    val elementScope: ElementScopeBase<Element>
) : RememberObserver, DomEffectScope {
    var onDispose: ((Element) -> Unit)? = null

    override fun onRemembered() {}

    override fun onForgotten() {
        onDispose?.invoke(elementScope.element)
    }

    override fun onAbandoned() {}

    override fun onDispose(disposeEffect: (Element) -> Unit) {
        onDispose = disposeEffect
    }
}