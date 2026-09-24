/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import kotlinx.browser.toKotlinString
import kotlinx.browser.toList
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLStyleElement
import kotlinx.browser.dom.Text
import kotlinx.browser.dom.css.CSSStyleSheet
import kotlinx.browser.dom.css.ElementCSSInlineStyle
import kotlinx.browser.dom.svg.SVGElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.attributes.toClassAttributeValue
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.StyleHolder
import org.jetbrains.compose.web.css.toStyleAttributeValue
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.HtmlValidationMode
import org.jetbrains.compose.web.internal.noncePropertyOrNull
import org.jetbrains.compose.web.internal.unsafeCast
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.browserDocument
import org.jetbrains.compose.web.internal.runtime.DomApplier
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.NamedEventListener

@Composable
@NonRestartableComposable
@ExplicitGroupsComposable
private inline fun <TScope, T> ComposeDomNode(
    crossinline factory: () -> T,
    elementScope: TScope,
    attrsUpdate: Updater<T>.() -> Unit,
    noinline content: (@Composable TScope.() -> Unit)?,
) {
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode {
            factory()
        }
    } else {
        currentComposer.useNode()
    }

    attrsUpdate.invoke(Updater(currentComposer))

    // The node and content lambda already provide the required composition groups.
    content?.invoke(elementScope)
    currentComposer.endNode()
}

// Compare DOM channels here while AttrsScopeBuilder keeps identity equality.
private class ElementAttrs(val builder: AttrsScopeBuilder<*>) {
    override fun equals(other: Any?): Boolean {
        if (other !is ElementAttrs) return false
        val a = builder
        val b = other.builder
        return a.collect() == b.collect() &&
            a.classes == b.classes &&
            a.styleScopeOrNull == b.styleScopeOrNull &&
            a.propertyUpdatesOrEmpty == b.propertyUpdatesOrEmpty &&
            a.eventsListenerScopeBuilder.collectListeners() ==
            b.eventsListenerScopeBuilder.collectListeners()
    }

    override fun hashCode(): Int = builder.collect().hashCode()
}

private fun Map<String, String>.containsAttribute(name: String): Boolean =
    name in this

// Cache validated class strings with copied keys and bounded growth.
private val validatedClassValues = mutableMapOf<List<String>, String>()

private fun classAttributeValue(classes: List<String>, validate: Boolean): String? {
    if (classes.isEmpty()) return null
    if (!validate) return classes.toClassAttributeValue(validate = false)
    validatedClassValues[classes]?.let { return it }
    val value = classes.toClassAttributeValue()!!
    if (validatedClassValues.size >= 256) validatedClassValues.clear()
    validatedClassValues[classes.toList()] = value
    return value
}

@ComposeWebInternalApi
private open class DomElementWrapper(override val node: Element) : DomNodeWrapper(node) {
    // Track each DOM channel so unchanged values are skipped.
    private var currentListeners = emptyList<NamedEventListener>()
    private var previousAttrs: AttrsScopeBuilder<*>? = null
    private var previousClasses: List<String>? = null
    private var previousStyle: StyleHolder? = null

    // Use one Compose update record while diffing DOM channels independently.
    fun updateElementAttrs(value: ElementAttrs) {
        val next = value.builder
        val previous = previousAttrs
        val attrs = next.collect()
        val classes = if (attrs.containsAttribute(AttrsScope.CLASS)) null else next.classes
        val style = if (attrs.containsAttribute("style")) {
            null
        } else {
            next.styleScopeOrNull ?: EmptyStyleScope
        }

        if (previous == null || previousClasses != classes) updateClasses(classes)
        if (previous == null || previousStyle != style) updateStyleDeclarations(style)
        if (previous == null || previous.collect() != attrs) updateAttrs(attrs)
        if (previous == null || previous.propertyUpdatesOrEmpty != next.propertyUpdatesOrEmpty) {
            updateProperties(next.propertyUpdatesOrEmpty)
        }
        updateEventListeners(next.eventsListenerScopeBuilder.collectListeners())

        previousAttrs = next
        previousClasses = classes
        previousStyle = style
    }

    protected fun eventListenersMatch(list: List<NamedEventListener>): Boolean =
        currentListeners == list

    open fun updateEventListeners(list: List<NamedEventListener>) {
        if (eventListenersMatch(list)) return

        currentListeners.forEach { listener ->
            node.removeEventListener(listener.name, listener.callback)
        }

        currentListeners = list

        currentListeners.forEach { listener ->
            node.addEventListener(listener.name, listener.callback)
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
        node.getAttributeNames().toList().forEach { jsName ->
            val name = jsName.toKotlinString()
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
            // Update the usual single Text child in place, with textContent as a fallback.
            val text = node.firstChild as? Text
            if (text != null && text.nextSibling == null) {
                text.data = value
            } else {
                node.textContent = value
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

// Reuse an empty style when clearing declarations removed by recomposition.
private val EmptyStyleScope = org.jetbrains.compose.web.css.StyleScopeBuilder()

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
        if (applier.validationMode == HtmlValidationMode.Fast && !allowance.isAllowed) return

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
            return
        }
        if (applier.validationMode == HtmlValidationMode.Fast && !allowance.isAllowed) return

        classes?.let {
            classAttributeValue(it, validate = applier.validationMode == HtmlValidationMode.Strict)
        }?.let { value ->
            verifyAttribute(AttrsScope.CLASS, value) {
                // Extra server classes are tolerated, so only missing ones are added.
                node.classList.add(*value.split(' ').toTypedArray())
            }
        }
    }

    override fun updateStyleDeclarations(declarations: StyleHolder?) {
        if (!applier.isHydrating) {
            super.updateStyleDeclarations(declarations)
            return
        }
        if ((applier.validationMode == HtmlValidationMode.Fast && !allowance.isAllowed) || declarations == null) return
        if (node !is HTMLElement && node !is SVGElement) return

        declarations.toStyleAttributeValue()?.let { value ->
            verifyAttribute("style", value) {
                super.updateStyleDeclarations(declarations)
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
        // SSR does not include listeners. A later deferred property can still throw after
        // these are attached, before disposal effects have started.
        applier.applyOrDeferDomMutation {
            applier.onAbortHydration { super.updateEventListeners(emptyList()) }
            super.updateEventListeners(list)
        }
    }

    override fun updateRawText(value: String) {
        // Allowed raw text uses the client value in both modes. Other initial server text
        // is retained in fast mode; later recompositions use the normal setter.
        if (!applier.isHydrating) {
            super.updateRawText(value)
        } else if (allowance.isAllowed) {
            applier.applyOrDeferDomMutation { super.updateRawText(value) }
        }
    }

    /** Reports a mismatch, or applies [patch] after hydration if the element allows it. */
    private fun verifyAttribute(name: String, expected: String?, patch: () -> Unit) {
        // Compare common HTML attributes in JavaScript to avoid Wasm string conversion.
        if (node.matchesHtmlAttribute(name, expected)) return
        val attribute = node.getComposedAttribute(name)
        // CSP hides the nonce attribute; older browsers may only expose the attribute.
        val actual = if (
            attribute != null &&
            node.namespaceURI == HtmlNamespace &&
            name == "nonce"
        ) {
            node.noncePropertyOrNull() ?: attribute
        } else {
            attribute
        }
        // Check exact equality before HTML-specific normalization.
        if (actual == expected) return
        if (
            expected != null &&
            name == AttrsScope.CLASS &&
            node.containsExpectedClasses(expected)
        ) {
            return
        }
        if (
            actual.normalizedForHydration(name, node.namespaceURI, node.localName) ==
            expected.normalizedForHydration(name, node.namespaceURI, node.localName)
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

// Boolean attributes have presence-only semantics on built-in HTML elements, but custom-element
// attributes keep their values even when their names match an HTML boolean attribute.
private fun String?.normalizedForHydration(
    attributeName: String,
    elementNamespace: String?,
    elementTagName: String,
): String? =
    if (
        this != null &&
        elementNamespace == HtmlNamespace &&
        '-' !in elementTagName &&
        attributeName.isHtmlBooleanAttributeName()
    ) "" else this

private fun String?.describeAttributeValue(): String =
    if (this == null) "no attribute" else "\"$this\""

private class DomElementScope<TElement : Element>(
    val hydrationAllowance: HydrationMismatchAllowance? = null,
) : ElementScopeImpl<TElement>() {
    lateinit var wrapper: DomElementWrapper
    var hasEventListeners: Boolean = false
}

internal actual val DefaultComposeHtmlContext: ComposeHtmlContext = BrowserComposeHtmlContext

@Composable
@NonRestartableComposable
private inline fun <TElement : Element> TagElementImpl(
    elementBuilder: ElementBuilder<TElement>,
    noinline applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    noinline content: (@Composable ElementScope<TElement>.() -> Unit)?,
    crossinline createWrapper: (TElement) -> DomElementWrapper,
    validateAttrs: (Map<String, String>) -> Unit = {},
    hydrationMismatchAllowance: HydrationMismatchAllowance? = null,
    updateElement: Updater<DomElementWrapper>.() -> Unit = {},
    providedScope: DomElementScope<TElement>? = null,
) {
    val scope = providedScope ?: remember { DomElementScope<TElement>() }

    // Compose compares one snapshot; the wrapper diffs its DOM channels.
    val attrsScope = AttrsScopeBuilder<TElement>()
    applyAttrs?.invoke(attrsScope)
    val refEffect = attrsScope.refEffect
    validateAttrs(attrsScope.collect())
    hydrationMismatchAllowance?.isAllowed = attrsScope.allowsHydrationMismatch
    scope.hasEventListeners = attrsScope.eventsListenerScopeBuilder.collectListeners().isNotEmpty()

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper>(
        factory = {
            val node = elementBuilder.create()
            scope.element = node
            createWrapper(node).also { wrapper -> scope.wrapper = wrapper }
        },
        attrsUpdate = {
            set(ElementAttrs(attrsScope), DomElementWrapper::updateElementAttrs)
            updateElement()
        },
        elementScope = scope,
        content = content,
    )

    // Create cleanup only for elements that installed listeners.
    if (scope.hasEventListeners) {
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

internal expect fun Element.matchesHtmlAttribute(name: String, expected: String?): Boolean

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
    @NonRestartableComposable
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
    @NonRestartableComposable
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
    @NonRestartableComposable
    override fun TextElement(value: String) {
        ComposeNode<DomNodeWrapper, DomApplier>(
            factory = { DomNodeWrapper(browserDocument.createTextNode("")) },
            update = {
                set(value) { newValue -> (node as Text).data = newValue }
            },
        )
    }

    @Composable
    @NonRestartableComposable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        TagElement(
            elementBuilder = elementBuilder("style"),
            applyAttrs = applyAttrs,
        ) {
            StyleSheetEffect(cssRules) { prepareStyleRawTextContent(cssRules) }
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
    @NonRestartableComposable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        val hydrationAwareBuilder = if (
            applier.isHydrating && elementBuilder !is HydratingElementBuilder<*>
        ) {
            val tagName = try {
                elementBuilder.tagName
            } catch (_: IllegalStateException) {
                throw HydrationMismatchException(
                    "Hydration requires tag-name element builders during the initial composition",
                )
            }
            HydratingElementBuilder(
                tagName = tagName,
                namespace = HtmlNamespace,
                applier = applier,
                browserBuilder = elementBuilder,
            )
        } else {
            elementBuilder
        }
        // Reuse the element scope for hydration allowance and avoid another group and object.
        val scope = remember { DomElementScope<TElement>(HydrationMismatchAllowance()) }
        val allowance = scope.hydrationAllowance!!
        // Keep scripted <noscript> fallback opaque during direct hydration.
        val preserveServerContent = remember {
            hydrationAwareBuilder is HydratingElementBuilder<*> &&
                hydrationAwareBuilder.preservesServerContent
        }
        TagElementImpl(
            elementBuilder = hydrationAwareBuilder,
            applyAttrs = applyAttrs,
            content = if (preserveServerContent) null else content,
            createWrapper = { node -> HydratingDomElementWrapper(node, applier, allowance) },
            hydrationMismatchAllowance = allowance,
            providedScope = scope,
        )
    }

    @Composable
    @NonRestartableComposable
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
    @NonRestartableComposable
    private fun <TElement : Element> HydratingTagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        allowance: HydrationMismatchAllowance,
        validateAttrs: (Map<String, String>) -> Unit = {},
        updateElement: Updater<DomElementWrapper>.() -> Unit = {},
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        // With scripting enabled, noscript contains the serialized fallback as raw text.
        // Keep that server-only content opaque, including during later recompositions.
        val preserveServerContent = remember {
            elementBuilder is HydratingElementBuilder<*> && elementBuilder.preservesServerContent
        }
        TagElementImpl(
            elementBuilder = elementBuilder,
            applyAttrs = applyAttrs,
            content = if (preserveServerContent) null else content,
            createWrapper = { node ->
                HydratingDomElementWrapper(node, applier, allowance)
            },
            validateAttrs = validateAttrs,
            hydrationMismatchAllowance = allowance,
            updateElement = updateElement,
        )
    }

    @Composable
    @NonRestartableComposable
    override fun TextElement(value: String) {
        ComposeNode<DomNodeWrapper, HydrationDomApplier>(
            factory = {
                val text = if (applier.isHydrating) {
                    applier.claimText(value)
                } else {
                    browserDocument.createTextNode("")
                }
                DomNodeWrapper(text)
            },
            update = {
                set(value) { newValue ->
                    applier.initializeText(node as Text, newValue)
                }
            },
        )
    }

    // A detached <style> has no sheet, so keep its CSS as text until it can use CSSOM.
    @Composable
    @NonRestartableComposable
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
            StyleSheetEffect(cssRules) { content.value }
        }
    }
}

// A detached <style> has no sheet. Keep CSS text until attachment makes CSSOM available.
@Composable
private fun ElementScope<HTMLStyleElement>.StyleSheetEffect(
    cssRules: CSSRuleDeclarationList,
    content: () -> RawTextContent,
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
            scopeElement.textContent = content().text
            onDispose {
                scopeElement.textContent = ""
            }
        }
    }
}

private class HydratingElementBuilder<TElement : Element>(
    override val tagName: String,
    private val namespace: String,
    private val applier: HydrationDomApplier,
    private val browserBuilder: ElementBuilder<TElement>,
    private val rawText: (() -> RawTextContent)? = null,
    private val allowance: HydrationMismatchAllowance? = null,
) : ElementBuilder<TElement> {
    val preservesServerContent: Boolean
        get() = tagName.equals("noscript", ignoreCase = true)

    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement = if (applier.isHydrating) {
        val rawTextProvider = rawText
        if (rawTextProvider == null) {
            applier.claimElement(tagName, namespace)
        } else {
            applier.claimElementWithRawText(
                tagName = tagName,
                namespace = namespace,
                rawText = rawTextProvider,
                allowContentMismatch = allowance?.isAllowed == true,
            )
        } as TElement
    } else {
        browserBuilder.create()
    }
}
