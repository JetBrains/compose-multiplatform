package androidx.compose.web.elements

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
import androidx.compose.web.DomApplier
import androidx.compose.web.DomElementWrapper
import androidx.compose.web.attributes.AttrsBuilder
import androidx.compose.web.attributes.Tag
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

@OptIn(ComposeCompilerApi::class)
@Composable
@ExplicitGroupsComposable
inline fun <TScope, T, reified E : Applier<*>> ComposeDomNode(
    noinline factory: () -> T,
    elementScope: TScope,
    noinline attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    content: @Composable TScope.() -> Unit
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
    content(elementScope)
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}

class DisposableEffectHolder(
    var effect: (DisposableEffectScope.(Element) -> DisposableEffectResult)? = null
)

@Composable
inline fun <TTag : Tag, TElement : Element> TagElement(
    tagName: String,
    crossinline applyAttrs: AttrsBuilder<TTag>.() -> Unit,
    content: @Composable ElementScope<TElement>.() -> Unit
) {
    val scope = remember { ElementScopeImpl<TElement>() }
    val refEffect = remember { DisposableEffectHolder() }

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper, DomApplier>(
        factory = {
            DomElementWrapper(document.createElement(tagName) as HTMLElement).also {
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
