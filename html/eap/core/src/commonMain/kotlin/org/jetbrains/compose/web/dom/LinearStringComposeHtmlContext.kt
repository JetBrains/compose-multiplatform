/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLStyleElement
import org.jetbrains.compose.web.HtmlValidationMode
import org.jetbrains.compose.web.LocalHtmlValidationMode
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.css.CSSRuleDeclarationList

/**
 * Builds HTML nodes as content executes instead of recording node factories and updates
 * in the slot table, as [StringComposeHtmlContext] does. The surrounding composition
 * handles locals, state, and effects.
 *
 * Each render starts with a fresh cursor. Keyed renders retain the tree and reuse nodes
 * only at matching positions with the same type (and, for elements, tag and namespace).
 */
internal class LinearStringComposeHtmlContext(private val root: StringHtmlElementNode) : ComposeHtmlContext {
    // The cursor points to the next child of the current parent.
    private var parent = root
    private var index = 0
    private var acceptingContent = true

    override val supportsDomElementAccess: Boolean = false

    override fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement> =
        StringElementBuilder(tagName)

    override fun <TElement : Element> elementBuilderNS(tagName: String, namespace: String): ElementBuilder<TElement> =
        StringElementBuilder(tagName, namespace)

    private fun put(node: StringHtmlNode) {
        check(acceptingContent) {
            "String rendering does not support HTML emission after composition"
        }
        if (index < parent.children.size) {
            parent.children[index] = node
        } else {
            parent.children.add(node)
        }
        index++
    }

    private fun trim() {
        // A reused parent may have children left over from the previous render.
        if (index < parent.children.size) {
            parent.children.subList(index, parent.children.size).clear()
        }
    }

    fun finish() {
        check(parent === root)
        trim()
        acceptingContent = false
    }

    @Composable
    @NonRestartableComposable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        val namespace = (elementBuilder as? StringElementBuilder<*>)?.namespace ?: HtmlNamespace
        val old = parent.children.getOrNull(index)
        // Reuse only the element at this position with the same tag and namespace.
        val node = if (
            old is StringHtmlElementNode &&
            old.tagName == elementBuilder.tagName &&
            old.namespace == namespace
        ) {
            old
        } else {
            StringHtmlElementNode(elementBuilder.tagName, namespace)
        }

        val attrs = AttrsScopeBuilder<TElement>()
        applyAttrs?.invoke(attrs)
        node.updateAttributes(
            attrs.stringAttributes(namespace, LocalHtmlValidationMode.current == HtmlValidationMode.Strict)
        )
        put(node)

        // Descend with a fresh child cursor, then resume the parent's cursor.
        val previousParent = parent
        val previousIndex = index
        parent = node
        index = 0
        content?.invoke(StringElementScope())
        trim()
        parent = previousParent
        index = previousIndex
    }

    @Composable
    @NonRestartableComposable
    override fun TextElement(value: String) {
        val old = parent.children.getOrNull(index)
        val node = if (old is StringHtmlTextNode) {
            old.apply { text = value }
        } else {
            StringHtmlTextNode(value)
        }
        put(node)
    }

    @Composable
    @NonRestartableComposable
    override fun <TElement : Element> RawTextElement(
        tagName: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: RawTextContent,
    ) {
        TagElement(elementBuilder<TElement>(tagName), applyAttrs) {
            val old = parent.children.getOrNull(index)
            val node = if (old is StringHtmlRawTextNode) {
                old.apply { this.content = content }
            } else {
                StringHtmlRawTextNode(content)
            }
            put(node)
        }
    }

    @Composable
    @NonRestartableComposable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        RawTextElement("style", applyAttrs, prepareStyleRawTextContent(cssRules))
    }
}
