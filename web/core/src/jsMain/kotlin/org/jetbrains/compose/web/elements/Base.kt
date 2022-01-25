package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.StyleHolder
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.NamedEventListener
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.ElementCSSInlineStyle
import org.w3c.dom.svg.SVGElement

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

@ComposeWebInternalApi
private class DomElementWrapper(override val node: Element): DomNodeWrapper(node) {
    private var currentListeners = emptyList<NamedEventListener>()

    fun updateEventListeners(list: List<NamedEventListener>) {
        currentListeners.forEach {
            node.removeEventListener(it.name, it)
        }

        currentListeners = list

        currentListeners.forEach {
            node.addEventListener(it.name, it)
        }
    }

    fun updateProperties(applicators: List<Pair<(Element, Any) -> Unit, Any>>) {
        node.removeAttribute("class")

        applicators.forEach { (applicator, item) ->
            applicator(node, item)
        }
    }

    fun updateStyleDeclarations(styleApplier: StyleHolder) {
        when (node) {
            is HTMLElement, is SVGElement -> {
                node.removeAttribute("style")

                val style = node.unsafeCast<ElementCSSInlineStyle>().style

                styleApplier.properties.forEach { (name, value) ->
                    style.setProperty(name, value.toString())
                }

                styleApplier.variables.forEach { (name, value) ->
                    style.setProperty(name, value.toString())
                }
            }
        }
    }

    fun updateAttrs(attrs: Map<String, String>) {
        node.getAttributeNames().forEach { name ->
            if (name == "style") return@forEach
            node.removeAttribute(name)
        }

        attrs.forEach {
            node.setAttribute(it.key, it.value)
        }
    }
}


@OptIn(ComposeWebInternalApi::class)
@Composable
fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsBuilder<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    val scope = remember {  ElementScopeImpl<TElement>() }
    var refEffect: (DisposableEffectScope.(TElement) -> DisposableEffectResult)? = null

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper>(
        factory = {
            val node = elementBuilder.create()
            scope.element = node
            DomElementWrapper(node)
        },
        attrsSkippableUpdate = {
            val attrsBuilder = AttrsBuilder<TElement>()
            applyAttrs?.invoke(attrsBuilder)

            refEffect = attrsBuilder.refEffect

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

    refEffect?.let { effect ->
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
