package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLStyleElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.StyleHolder
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomApplier
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.NamedEventListener
import org.w3c.dom.HTMLElement
import org.w3c.dom.Text
import org.w3c.dom.css.ElementCSSInlineStyle
import org.w3c.dom.css.CSSStyleSheet
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
                    setVariable(style, name, value)
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

internal actual val DefaultComposeHtmlContext: ComposeHtmlContext = BrowserComposeHtmlContext

@OptIn(ComposeWebInternalApi::class)
private object BrowserComposeHtmlContext : ComposeHtmlContext {
    override val supportsDomElementAccess: Boolean = true

    override fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement> =
        ElementBuilder.createBuilder(tagName)

    @Composable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        val scope = remember { ElementScopeImpl<TElement>() }
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

    @Composable
    override fun TextElement(value: String) {
        ComposeNode<DomNodeWrapper, DomApplier>(
            factory = { DomNodeWrapper(document.createTextNode("")) },
            update = {
                set(value) { newValue -> (node as Text).data = newValue }
            },
        )
    }

    @Composable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        TagElement(
            elementBuilder = elementBuilder("style"),
            applyAttrs = applyAttrs,
        ) {
            DisposableEffect(cssRules, cssRules.size) {
                val cssStylesheet = scopeElement.sheet as? CSSStyleSheet
                cssStylesheet?.setCSSRules(cssRules)
                onDispose {
                    cssStylesheet?.clearCSSRules()
                }
            }
        }
    }
}

internal fun hydratingComposeHtmlContext(
    applier: HydrationDomApplier,
): ComposeHtmlContext = HydratingComposeHtmlContext(applier)

private class HydratingComposeHtmlContext(
    private val applier: HydrationDomApplier,
) : ComposeHtmlContext by BrowserComposeHtmlContext {
    override fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement> =
        HydratingElementBuilder(
            tagName = tagName,
            applier = applier,
            browserBuilder = ElementBuilder.createBuilder(tagName),
        )

    @Composable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        if (applier.isHydrating && elementBuilder !is HydratingElementBuilder<*>) {
            throw HydrationMismatchException(
                "Hydration requires tag-name element builders during the initial composition",
            )
        }
        BrowserComposeHtmlContext.TagElement(elementBuilder, applyAttrs, content)
    }

    @Composable
    override fun TextElement(value: String) {
        ComposeNode<DomNodeWrapper, HydrationDomApplier>(
            factory = {
                val text = if (applier.isHydrating) {
                    applier.claimText(value)
                } else {
                    document.createTextNode("")
                }
                DomNodeWrapper(text)
            },
            update = {
                set(value) { newValue -> (node as Text).data = newValue }
            },
        )
    }

    @Composable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        if (applier.isHydrating) {
            throw UnsupportedOperationException(
                "Style elements are not supported yet",
            )
        }
        BrowserComposeHtmlContext.StyleElement(applyAttrs, cssRules)
    }
}

private class HydratingElementBuilder<TElement : Element>(
    private val tagName: String,
    private val applier: HydrationDomApplier,
    private val browserBuilder: ElementBuilder<TElement>,
) : ElementBuilder<TElement> {
    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement = if (applier.isHydrating) {
        applier.claimElement(tagName) as TElement
    } else {
        browserBuilder.create()
    }
}
