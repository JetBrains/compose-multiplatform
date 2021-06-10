package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.SkippableUpdater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.DomApplier
import org.jetbrains.compose.web.DomElementWrapper
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.Tag
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement

@OptIn(ComposeCompilerApi::class)
@Composable
@ExplicitGroupsComposable
inline fun <TScope, T, reified E : Applier<*>> ComposeDomNode(
    noinline factory: () -> T,
    elementScope: TScope,
    noinline attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    noinline content: (@Composable TScope.() -> Unit)?
) {
    if (currentComposer.applier !is E) error("Invalid applier")
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode(factory)
    } else {
        currentComposer.useNode()
    }

    SkippableUpdater<T>(currentComposer).apply {
        attrsSkippableUpdate()
    }

    currentComposer.startReplaceableGroup(0x7ab4aae9)
    content?.invoke(elementScope)
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}

class DisposableEffectHolder(
    var effect: (DisposableEffectScope.(Element) -> DisposableEffectResult)? = null
)

class ElementBuilder<THTMLElement : HTMLElement>(private val tagName: String) {
    private val el: THTMLElement by lazy { document.createElement(tagName) as THTMLElement }
    fun create() = el.cloneNode(true) as THTMLElement

    companion object {
        val Div = ElementBuilder<HTMLDivElement>("div")
        val A = ElementBuilder<HTMLAnchorElement>("a")
        val Input = ElementBuilder<HTMLInputElement>("input")
        val Button = ElementBuilder<HTMLButtonElement>("button")
        val Span = ElementBuilder<HTMLSpanElement>("span")
    }
}

@Composable
fun <TTag : Tag, TElement : Element> TagElement(
    elementBuilder: () -> HTMLElement,
    applyAttrs: AttrsBuilder<TTag>.() -> Unit,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    val scope = remember { ElementScopeImpl<TElement>() }
    val refEffect = remember { DisposableEffectHolder() }

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper, DomApplier>(
        factory = {
            DomElementWrapper(elementBuilder()).also {
                scope.element = it.node.unsafeCast<TElement>()
            }
        },
        attrsSkippableUpdate = {
            val attrsApplied = AttrsBuilder<TTag>().also { it.applyAttrs() }
            refEffect.effect = attrsApplied.refEffect
            val attrsCollected = attrsApplied.collect()
            val events = attrsApplied.collectListeners()

            update {
                set(attrsCollected, DomElementWrapper::updateAttrs)
                set(events, DomElementWrapper::updateEventListeners)
                set(attrsApplied.propertyUpdates, DomElementWrapper::updateProperties)
                set(attrsApplied.styleBuilder, DomElementWrapper::updateStyleDeclarations)
            }
        },
        elementScope = scope,
        content = content
    )

    DisposableEffect(null) {
        refEffect.effect?.invoke(this, scope.element) ?: onDispose {}
    }
}


@Composable
fun <TTag : Tag, TElement : Element> TagElement(
    tagName: String,
    applyAttrs: AttrsBuilder<TTag>.() -> Unit,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
)  = TagElement(
    elementBuilder = {  document.createElement(tagName) as HTMLElement },
    applyAttrs = applyAttrs,
    content = content
)
