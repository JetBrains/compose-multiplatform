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
import androidx.compose.web.DomNodeWrapper
import androidx.compose.web.attributes.AttrsBuilder
import androidx.compose.web.attributes.Tag
import androidx.compose.web.css.StyleBuilder
import androidx.compose.web.css.StyleBuilderImpl
import kotlinx.browser.document
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
    var effect: (DisposableEffectScope.(HTMLElement) -> DisposableEffectResult)? = null
)

@Composable
inline fun <TTag : Tag, THTMLElement : HTMLElement> TagElement(
    tagName: String,
    crossinline applyAttrs: AttrsBuilder<TTag>.() -> Unit,
    content: @Composable ElementScope<THTMLElement>.() -> Unit
) {
    val scope = remember { ElementScopeImpl<THTMLElement>() }
    val refEffect = remember { DisposableEffectHolder() }

    ComposeDomNode<ElementScope<THTMLElement>, DomNodeWrapper, DomApplier>(
        factory = {
            DomNodeWrapper(document.createElement(tagName)).also {
                scope.element = it.node.unsafeCast<THTMLElement>()
            }
        },
        attrsSkippableUpdate = {
            val attrsApplied = AttrsBuilder<TTag>().also { it.applyAttrs() }
            refEffect.effect = attrsApplied.refEffect
            val attrsCollected = attrsApplied.collect()
            val events = attrsApplied.collectListeners()

            update {
                set(attrsCollected, DomNodeWrapper.UpdateAttrs)
                set(events, DomNodeWrapper.UpdateListeners)
                set(attrsApplied.propertyUpdates, DomNodeWrapper.UpdateProperties)
                set(attrsApplied.styleBuilder, DomNodeWrapper.UpdateStyleDeclarations)
            }
        },
        elementScope = scope,
        content = content
    )

    DisposableEffect(null) {
        refEffect.effect?.invoke(this, scope.element) ?: onDispose {}
    }
}
