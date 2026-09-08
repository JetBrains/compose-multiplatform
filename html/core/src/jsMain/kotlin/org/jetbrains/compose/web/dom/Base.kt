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
            if (node.getComposedAttribute(name) != value) {
                node.setComposedAttribute(name, value)
            }
        }
    }

    open fun updateRawText(value: String) {
        if (node.textContent != value) {
            node.textContent = value
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
    private val allowance: HydrationMismatchAllowance,
) : DomElementWrapper(node), HydrationMismatchAware {
    override val allowsHydrationMismatch: Boolean
        get() = allowance.isAllowed

    override fun updateAttrs(attrs: Map<String, String>) {
        if (!applier.isHydrating) {
            super.updateAttrs(attrs)
            return
        }

        attrs.forEach { (name, value) ->
            verifyAttribute(name, expected = value) {
                // Unrelated server attributes are tolerated, so only the composed one is patched.
                node.setComposedAttribute(name, value)
            }
        }
    }

    override fun updateClasses(classes: List<String>?) {
        if (!applier.isHydrating) {
            super.updateClasses(classes)
        } else {
            classes?.toClassAttributeValue()?.let { value ->
                verifyAttribute(AttrsScope.CLASS, value) {
                    // Extra server classes are tolerated, so only missing ones are added.
                    node.classList.add(*value.split(' ').toTypedArray())
                }
            }
        }
    }

    override fun updateStyleDeclarations(declarations: StyleHolder?) {
        if (!applier.isHydrating) {
            super.updateStyleDeclarations(declarations)
        } else if (declarations != null && (node is HTMLElement || node is SVGElement)) {
            declarations.toStyleAttributeValue()?.let { value ->
                verifyAttribute("style", value) {
                    super.updateStyleDeclarations(declarations)
                }
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

    override fun updateRawText(value: String) {
        // HydratingElementBuilder already verified the claimed raw text. Preserve that server DOM
        // node during the initial update; later recompositions use the normal setter.
        if (!applier.isHydrating) {
            super.updateRawText(value)
        } else if (allowance.isAllowed) {
            applier.applyOrDeferDomMutation { super.updateRawText(value) }
        }
    }

    /** Reports a mismatch, or applies [patch] after hydration if the element allows it. */
    private fun verifyAttribute(name: String, expected: String?, patch: () -> Unit) {
        val actual = node.getComposedAttribute(name)
        if (
            expected != null &&
            (name == AttrsScope.CLASS ||
                node.namespaceURI == HtmlNamespace && name.asciiLowercase() == AttrsScope.CLASS) &&
            node.containsExpectedClasses(expected)
        ) {
            return
        }
        if (
            actual.normalizedForHydration(name, node.namespaceURI) ==
            expected.normalizedForHydration(name, node.namespaceURI)
        ) return
        if (allowance.isAllowed) {
            applier.applyOrDeferDomMutation(patch)
            return
        }
        applier.mismatch(
            "attribute \"$name\": expected ${expected.describeAttributeValue()}, " +
                "found ${actual.describeAttributeValue()}",
        )
    }
}

// These are the foreign attributes whose namespaces are assigned by the HTML parser.
private fun Element.composedAttributeNamespace(name: String): String? =
    if (namespaceURI == HtmlNamespace) null else when (name) {
        "xlink:actuate", "xlink:arcrole", "xlink:href", "xlink:role", "xlink:show",
        "xlink:title", "xlink:type" -> "http://www.w3.org/1999/xlink"
        "xml:base", "xml:lang", "xml:space" -> "http://www.w3.org/XML/1998/namespace"
        "xmlns", "xmlns:xlink" -> "http://www.w3.org/2000/xmlns/"
        else -> null
    }

private fun Element.getComposedAttribute(name: String): String? {
    val namespace = composedAttributeNamespace(name)
    // A qualified-name lookup alone would also accept an attribute in the wrong namespace.
    return if (namespace == null) getAttribute(name) else {
        getAttributeNS(namespace, name.substringAfter(':'))
    }
}

private fun Element.setComposedAttribute(name: String, value: String) {
    val namespace = composedAttributeNamespace(name)
    if (namespace == null) {
        setAttribute(name, value)
    } else {
        // Also replace an incorrectly unnamespaced attribute when patching a hydration mismatch.
        removeAttribute(name)
        setAttributeNS(namespace, name, value)
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

// Boolean attributes have presence-only semantics in HTML, but are ordinary attributes in SVG.
private fun String?.normalizedForHydration(
    attributeName: String,
    elementNamespace: String?,
): String? =
    if (
        this != null &&
        elementNamespace == HtmlNamespace &&
        attributeName.isHtmlBooleanAttributeName()
    ) "" else this

private fun String?.describeAttributeValue(): String =
    if (this == null) "no attribute" else "\"$this\""

private class DomElementScope<TElement : Element> : ElementScopeImpl<TElement>() {
    lateinit var wrapper: DomElementWrapper
}

internal actual val DefaultComposeHtmlContext: ComposeHtmlContext = BrowserComposeHtmlContext

// Attribute updates are prepared before the DOM node exists. Select the namespace-specific
// fallback only when applying the update, and include both alternatives in Compose's change check.
private data class AttributeFallback<T>(val html: T?, val foreign: T?) {
    fun forNamespace(namespace: String?): T? = if (namespace == HtmlNamespace) html else foreign
}

private fun <T> Map<String, String>.attributeFallback(name: String, value: T): AttributeFallback<T> =
    AttributeFallback(
        html = value.takeUnless { containsAttribute(name, HtmlNamespace) },
        foreign = value.takeUnless { name in this },
    )

@Composable
private fun <TElement : Element> TagElementImpl(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
    createWrapper: (TElement) -> DomElementWrapper,
    validateAttrs: (Map<String, String>) -> Unit = {},
    hydrationMismatchAllowance: HydrationMismatchAllowance? = null,
    updateElement: Updater<DomElementWrapper>.() -> Unit = {},
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
            validateAttrs(attrs)
            // Composition completes before the DOM is claimed, which is when this is read.
            hydrationMismatchAllowance?.isAllowed = attrsScope.allowsHydrationMismatch

            update {
                set(attrs.attributeFallback(AttrsScope.CLASS, attrsScope.classes)) { fallback ->
                    updateClasses(fallback.forNamespace(node.namespaceURI))
                }
                set(attrs.attributeFallback("style", attrsScope.styleScope)) { fallback ->
                    updateStyleDeclarations(fallback.forNamespace(node.namespaceURI))
                }
                set(attrs, DomElementWrapper::updateAttrs)
                updateElement()
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

    override fun <TElement : Element> elementBuilderNS(
        tagName: String,
        namespace: String,
    ): ElementBuilder<TElement> = ElementBuilder.createBuilder(tagName, namespace)

    @Composable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        TagElementImpl(
            elementBuilder = elementBuilder,
            applyAttrs = applyAttrs,
            content = content,
            createWrapper = ::DomElementWrapper,
        )
    }

    @Composable
    override fun <TElement : Element> RawTextElement(
        tagName: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: RawTextContent,
    ) {
        TagElementImpl(
            elementBuilder = elementBuilder(tagName),
            applyAttrs = applyAttrs,
            content = null,
            createWrapper = ::DomElementWrapper,
            validateAttrs = content::validateAttributes,
            updateElement = {
                set(content.text, DomElementWrapper::updateRawText)
            },
        )
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
            namespace = HtmlNamespace,
            applier = applier,
            browserBuilder = ElementBuilder.createBuilder(tagName),
        )

    override fun <TElement : Element> elementBuilderNS(
        tagName: String,
        namespace: String,
    ): ElementBuilder<TElement> = HydratingElementBuilder(
        tagName = tagName,
        namespace = namespace,
        applier = applier,
        browserBuilder = ElementBuilder.createBuilder(tagName, namespace),
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
        HydratingTagElement(
            elementBuilder = elementBuilder,
            applyAttrs = applyAttrs,
            allowance = remember { HydrationMismatchAllowance() },
            content = content,
        )
    }

    @Composable
    override fun <TElement : Element> RawTextElement(
        tagName: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: RawTextContent,
    ) {
        // Raw text is claimed by the element builder, before the wrapper exists, so both share it.
        val allowance = remember { HydrationMismatchAllowance() }
        val rawTextElementBuilder = HydratingElementBuilder<TElement>(
            tagName = tagName,
            namespace = HtmlNamespace,
            applier = applier,
            browserBuilder = ElementBuilder.createBuilder(tagName),
            rawText = { content },
            allowance = allowance,
        )
        HydratingTagElement(
            elementBuilder = rawTextElementBuilder,
            applyAttrs = applyAttrs,
            allowance = allowance,
            validateAttrs = content::validateAttributes,
            updateElement = {
                set(content.text, DomElementWrapper::updateRawText)
            },
            content = null,
        )
    }

    @Composable
    private fun <TElement : Element> HydratingTagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        allowance: HydrationMismatchAllowance,
        validateAttrs: (Map<String, String>) -> Unit = {},
        updateElement: Updater<DomElementWrapper>.() -> Unit = {},
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        TagElementImpl(
            elementBuilder = elementBuilder,
            applyAttrs = applyAttrs,
            content = content,
            createWrapper = { node ->
                HydratingDomElementWrapper(node, applier, allowance)
            },
            validateAttrs = validateAttrs,
            hydrationMismatchAllowance = allowance,
            updateElement = updateElement,
        )
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
                set(value) { newValue ->
                    val text = node as Text
                    // Claimed text already holds the server value unless its element allows
                    // mismatches. Defer that patch, so a later mismatch can still fall back.
                    if (text.data != newValue) {
                        applier.applyOrDeferDomMutation { text.data = newValue }
                    }
                }
            },
        )
    }

    // A detached <style> has no sheet, so keep its CSS as text until it can use CSSOM.
    @Composable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        val content = remember(cssRules, cssRules.size) {
            lazy { prepareStyleRawTextContent(cssRules) }
        }
        val allowance = remember { HydrationMismatchAllowance() }
        HydratingTagElement<HTMLStyleElement>(
            elementBuilder = HydratingElementBuilder(
                tagName = "style",
                namespace = HtmlNamespace,
                applier = applier,
                browserBuilder = ElementBuilder.createBuilder("style"),
                rawText = { content.value },
                allowance = allowance,
            ),
            applyAttrs = applyAttrs,
            allowance = allowance,
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
                    scopeElement.textContent = content.value.text
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
    private val namespace: String,
    private val applier: HydrationDomApplier,
    private val browserBuilder: ElementBuilder<TElement>,
    private val rawText: (() -> RawTextContent)? = null,
    private val allowance: HydrationMismatchAllowance? = null,
) : ElementBuilder<TElement> {
    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement = if (applier.isHydrating) {
        if (rawText == null) {
            applier.claimElement(tagName, namespace)
        } else {
            applier.claimElementWithRawText(
                tagName = tagName,
                namespace = namespace,
                value = rawText().text,
                allowContentMismatch = allowance?.isAllowed == true,
            )
        } as TElement
    } else {
        browserBuilder.create()
    }
}
