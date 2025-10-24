package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.css.StyleHolder
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.NamedEventListener
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.ElementCSSInlineStyle
import org.w3c.dom.svg.SVGElement

@Composable
@ExplicitGroupsComposable
private inline fun <TScope, T> ComposeDomNode(
    crossinline factory: () -> T,
    elementScope: TScope,
    attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    content: (@Composable TScope.() -> Unit)
) {
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode {
            factory()
        }
    } else {
        currentComposer.useNode()
    }

    attrsSkippableUpdate.invoke(SkippableUpdater(currentComposer))

    currentComposer.startReplaceableGroup(0x7ab4aae9)
    content.invoke(elementScope)
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
        applicators.forEach { (applicator, item) ->
            applicator(node, item)
        }
    }

    fun updateStyleDeclarations(styleApplier: StyleHolder) {
        when (node) {
            is HTMLElement, is SVGElement -> {
                node.removeAttribute("style")

                val style = node.unsafeCast<ElementCSSInlineStyle>().style

                styleApplier.properties.forEach { (name, value, important) ->
                    style.setProperty(name, value.toString(), if (important) "important" else "")
                }

                styleApplier.variables.forEach { (name, value) ->
                    style.setProperty(name, value.toString())
                }
            }
        }
    }

    fun updateAttrs(attrs: Map<String, String>) {
        node.getAttributeNames().forEach { name ->
            when (name) {
                "style", "class" -> {
                    // skip style and class here, they're managed in corresponding methods
                }
                else -> node.removeAttribute(name)
            }
        }

        attrs.forEach {
            node.setAttribute(it.key, it.value)
        }
    }

    fun updateClasses(classes: List<String>) {
        node.removeAttribute("class")
        if (classes.isNotEmpty()) {
            node.classList.add(*classes.toTypedArray())
        }
    }
}


@OptIn(ComposeWebInternalApi::class)
@Composable
fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
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
            val attrsScope = AttrsScopeBuilder<TElement>()
            applyAttrs?.invoke(attrsScope)

            refEffect = attrsScope.refEffect

            update {
                set(attrsScope.classes, DomElementWrapper::updateClasses)
                set(attrsScope.styleScope, DomElementWrapper::updateStyleDeclarations)
                set(attrsScope.collect(), DomElementWrapper::updateAttrs)
                set(
                    attrsScope.eventsListenerScopeBuilder.collectListeners(),
                    DomElementWrapper::updateEventListeners
                )
                set(attrsScope.propertyUpdates, DomElementWrapper::updateProperties)
            }
        },
        elementScope = scope,
        content = {
            content?.invoke(this)
        }
    )

    refEffect?.let { effect ->
        DisposableEffect(null) {
            effect.invoke(this, scope.element)
        }
    }
}

/**
 * @param tagName - the name of the tag that needs to be created.
 * It's best to use constant values for [tagName].
 * If variable [tagName] needed, consider wrapping TagElement calls into an if...else:
 *
 * ```
 *      if (useDiv) TagElement("div",...) else TagElement("span", ...)
 * ```
 */
@Composable
fun <TElement : Element> TagElement(
    tagName: String,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    key(tagName) {
        TagElement(
            elementBuilder = ElementBuilder.createBuilder(tagName),
            applyAttrs = applyAttrs,
            content = content
        )
    }
}
