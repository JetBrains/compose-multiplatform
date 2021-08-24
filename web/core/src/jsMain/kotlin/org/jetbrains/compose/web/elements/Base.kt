package org.jetbrains.compose.web.dom

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

    attrsSkippableUpdate.invoke(SkippableUpdater(currentComposer))

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
            val attrsBuilder = AttrsBuilder<TElement>()
            applyAttrs?.invoke(attrsBuilder)

            refEffect.effect = attrsBuilder.refEffect

            update {
                set(attrsBuilder.collect(), DomElementWrapper::updateAttrs)
                set(attrsBuilder.collectListeners(), DomElementWrapper::updateEventListeners)
                set(attrsBuilder.propertyUpdates, DomElementWrapper::updateProperties)
                set(attrsBuilder.styleBuilder, DomElementWrapper::updateStyleDeclarations)
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

@OptIn(ComposeWebInternalApi::class)
private fun DomElementWrapper.updateProperties(applicators: List<Pair<(Element, Any) -> Unit, Any>>) {
    if (node.className.isNotEmpty()) node.className = ""

    applicators.forEach { (applicator, item) ->
        applicator(node, item)
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
