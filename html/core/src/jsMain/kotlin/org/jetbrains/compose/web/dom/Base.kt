package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLStyleElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.attributes.toClassAttributeValue
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.StyleHolder
import org.jetbrains.compose.web.css.toStyleAttributeValue
import org.jetbrains.compose.web.css.utils.serializeRules
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
private open class DomElementWrapper(override val node: Element) : DomNodeWrapper(node) {
    private var currentListeners = emptyList<NamedEventListener>()

    protected fun eventListenersMatch(list: List<NamedEventListener>): Boolean =
        currentListeners == list

    open fun updateEventListeners(list: List<NamedEventListener>) {
        if (eventListenersMatch(list)) return

        currentListeners.forEach { listener ->
            node.removeEventListener(listener.name, listener)
        }

        currentListeners = list

        currentListeners.forEach { listener ->
            node.addEventListener(listener.name, listener)
        }
    }

    open fun updateProperties(applicators: List<Pair<(Element, Any) -> Unit, Any>>) {
        applicators.forEach { (applicator, item) ->
            applicator(node, item)
        }
    }

    open fun updateStyleDeclarations(declarations: StyleHolder?) {
        if (declarations == null || (node !is HTMLElement && node !is SVGElement)) return

        node.removeAttribute("style")
        val style = node.unsafeCast<ElementCSSInlineStyle>().style

        declarations.properties.forEach { (name, value, important) ->
            style.setProperty(name, value.toString(), if (important) "important" else "")
        }

        declarations.variables.forEach { (name, value) ->
            setVariable(style, name, value)
        }
    }

    open fun updateAttrs(attrs: Map<String, String>) {
        node.getAttributeNames().forEach { name ->
            if (name != "style" && name != AttrsScope.CLASS && name !in attrs) {
                node.removeAttribute(name)
            }
        }

        attrs.forEach { (name, value) ->
            if (node.getAttribute(name) != value) {
                node.setAttribute(name, value)
            }
        }
    }

    open fun updateClasses(classes: List<String>?) {
        if (classes == null) return
        node.removeAttribute(AttrsScope.CLASS)
        if (classes.isNotEmpty()) {
            node.classList.add(*classes.toTypedArray())
        }
    }
}

@ComposeWebInternalApi
private class HydratingDomElementWrapper(
    node: Element,
    private val applier: HydrationDomApplier,
) : DomElementWrapper(node) {
    override fun updateAttrs(attrs: Map<String, String>) {
        if (!applier.isHydrating) {
            super.updateAttrs(attrs)
            return
        }

        attrs.forEach { (name, value) -> verifyAttribute(name, expected = value) }
    }

    override fun updateClasses(classes: List<String>?) {
        if (!applier.isHydrating) {
            super.updateClasses(classes)
        } else {
            classes?.toClassAttributeValue()?.let { value ->
                verifyAttribute(AttrsScope.CLASS, value)
            }
        }
    }

    override fun updateStyleDeclarations(declarations: StyleHolder?) {
        if (!applier.isHydrating) {
            super.updateStyleDeclarations(declarations)
        } else if (declarations != null && (node is HTMLElement || node is SVGElement)) {
            declarations.toStyleAttributeValue()?.let { value ->
                verifyAttribute("style", value)
            }
        }
    }

    override fun updateProperties(applicators: List<Pair<(Element, Any) -> Unit, Any>>) {
        if (applicators.isEmpty()) return
        applier.applyOrDeferDomMutation {
            super.updateProperties(applicators)
        }
    }

    override fun updateEventListeners(list: List<NamedEventListener>) {
        if (eventListenersMatch(list)) return
        // SSR does not include listeners. Attach them only after hydration succeeds, so a
        // mismatch cannot leave listeners attached to the discarded server-rendered element.
        applier.applyOrDeferDomMutation {
            super.updateEventListeners(list)
        }
    }

    private fun verifyAttribute(name: String, expected: String?) {
        val actual = node.getAttribute(name)
        if (
            expected != null &&
            name.equals(AttrsScope.CLASS, ignoreCase = true) &&
            node.containsExpectedClasses(expected)
        ) {
            return
        }
        if (actual.normalizedForHydration(name) == expected.normalizedForHydration(name)) return
        applier.mismatch(
            "attribute \"$name\": expected ${expected.describeAttributeValue()}, " +
                "found ${actual.describeAttributeValue()}",
        )
    }
}

private fun Element.containsExpectedClasses(expected: String): Boolean {
    val expectedClasses = expected
        .split(' ', '\t', '\n', '\r', '\u000C')
        .filter(String::isNotEmpty)
    return if (expectedClasses.isEmpty()) {
        hasAttribute(AttrsScope.CLASS)
    } else {
        expectedClasses.all(classList::contains)
    }
}

private fun String?.normalizedForHydration(attributeName: String): String? =
    if (this != null && attributeName.isHtmlBooleanAttributeName()) "" else this

private fun String?.describeAttributeValue(): String =
    if (this == null) "no attribute" else "\"$this\""

private class DomElementScope<TElement : Element> : ElementScopeImpl<TElement>() {
    lateinit var wrapper: DomElementWrapper
}

internal actual val DefaultComposeHtmlContext: ComposeHtmlContext = BrowserComposeHtmlContext

@Composable
private fun <TElement : Element> TagElementImpl(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
    createWrapper: (TElement) -> DomElementWrapper,
) {
    val scope = remember { DomElementScope<TElement>() }
    var refEffect: (DisposableEffectScope.(TElement) -> DisposableEffectResult)? = null

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper>(
        factory = {
            val node = elementBuilder.create()
            scope.element = node
            createWrapper(node).also { wrapper -> scope.wrapper = wrapper }
        },
        attrsSkippableUpdate = {
            val attrsScope = AttrsScopeBuilder<TElement>()
            applyAttrs?.invoke(attrsScope)

            refEffect = attrsScope.refEffect
            val attrs = attrsScope.collect()

            update {
                set(
                    attrsScope.classes.takeUnless { AttrsScope.CLASS in attrs },
                    DomElementWrapper::updateClasses,
                )
                set(
                    attrsScope.styleScope.takeUnless { "style" in attrs },
                    DomElementWrapper::updateStyleDeclarations,
                )
                set(attrs, DomElementWrapper::updateAttrs)
                set(attrsScope.propertyUpdates, DomElementWrapper::updateProperties)
                set(
                    attrsScope.eventsListenerScopeBuilder.collectListeners(),
                    DomElementWrapper::updateEventListeners,
                )
            }
        },
        elementScope = scope,
        content = {
            content?.invoke(this)
        },
    )

    if (applyAttrs != null) {
        DisposableEffect(Unit) {
            onDispose {
                scope.wrapper.updateEventListeners(emptyList())
            }
        }
    }

    refEffect?.let { effect ->
        DisposableEffect(null) {
            effect.invoke(this, scope.element)
        }
    }
}

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
        TagElementImpl(elementBuilder, applyAttrs, content, ::DomElementWrapper)
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
        TagElementImpl(elementBuilder, applyAttrs, content) { node ->
            HydratingDomElementWrapper(node, applier)
        }
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

    // A detached <style> has no sheet, so keep its CSS as text until it can use CSSOM.
    @Composable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        TagElement<HTMLStyleElement>(
            elementBuilder = HydratingElementBuilder(
                tagName = "style",
                applier = applier,
                browserBuilder = ElementBuilder.createBuilder("style"),
                rawText = { cssRules.serializeRules().joinToString("\n") },
            ),
            applyAttrs = applyAttrs,
        ) {
            DisposableEffect(cssRules, cssRules.size) {
                if (scopeElement.sheet is CSSStyleSheet) {
                    // Remove SSR or fallback text once; later updates must keep the current sheet.
                    if (scopeElement.firstChild != null) {
                        scopeElement.textContent = ""
                    }
                    // Clearing the text replaces the stylesheet, so get the new sheet afterwards.
                    val cssStylesheet = scopeElement.sheet as? CSSStyleSheet
                    cssStylesheet?.setCSSRules(cssRules)
                    onDispose {
                        cssStylesheet?.clearCSSRules()
                    }
                } else {
                    scopeElement.textContent = cssRules.serializeRules().joinToString("\n")
                    onDispose {
                        scopeElement.textContent = ""
                    }
                }
            }
        }
    }
}

private class HydratingElementBuilder<TElement : Element>(
    private val tagName: String,
    private val applier: HydrationDomApplier,
    private val browserBuilder: ElementBuilder<TElement>,
    private val rawText: (() -> String)? = null,
) : ElementBuilder<TElement> {
    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement = if (applier.isHydrating) {
        if (rawText == null) {
            applier.claimElement(tagName)
        } else {
            applier.claimElementWithRawText(tagName, rawText())
        } as TElement
    } else {
        browserBuilder.create()
    }
}
