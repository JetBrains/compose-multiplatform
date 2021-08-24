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
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.internal.runtime.DomApplier
import org.jetbrains.compose.web.internal.runtime.DomElementWrapper
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

@OptIn(ComposeCompilerApi::class)
@Composable
@ExplicitGroupsComposable
private inline fun <TScope, T> ComposeDomNode(
    noinline factory: () -> T,
    elementScope: TScope,
    noinline attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    noinline content: (@Composable TScope.() -> Unit)?
) {
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

class DisposableEffectHolder<TElement : Element>(
    var effect: (DisposableEffectScope.(TElement) -> DisposableEffectResult)? = null
)

@OptIn(ComposeWebInternalApi::class)
@Composable
fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsBuilder<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    val scope = remember { ElementScopeImpl<TElement>() }
    val refEffect = remember { DisposableEffectHolder<TElement>() }

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper>(
        factory = {
            DomElementWrapper(elementBuilder.create() as HTMLElement).also {
                scope.element = it.node.unsafeCast<TElement>()
            }
        },
        attrsSkippableUpdate = {
            val attrsApplied = AttrsBuilder<TElement>().also {
                if (applyAttrs != null) {
                    it.applyAttrs()
                }
            }
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

    refEffect.effect?.let { effect ->
        DisposableEffect(null) {
            effect.invoke(this, scope.element)
        }
    }
}

@Composable
@ExperimentalComposeWebApi
fun <TElement : Element> TagElement(
    tagName: String,
    applyAttrs: AttrsBuilder<TElement>.() -> Unit,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) = TagElement(
    elementBuilder = ElementBuilder.createBuilder(tagName),
    applyAttrs = applyAttrs,
    content = content
)
