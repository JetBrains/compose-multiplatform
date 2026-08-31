package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.SkippableUpdater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLStyleElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.attributes.toClassAttributeValue
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.toStyleAttributeValue
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

internal object StringComposeHtmlContext : ComposeHtmlContext {
    override val supportsDomElementAccess: Boolean = false

    override fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement> =
        StringElementBuilder(tagName)

    @Composable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) = StringTagElement(
        elementBuilder = elementBuilder,
        applyAttrs = applyAttrs,
        content = content,
    )

    @Composable
    private fun <TElement : Element> StringTagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
        rawText: RawTextContent? = null,
    ) {
        val stringElementBuilder = elementBuilder as? StringElementBuilder<*>
            ?: error(
                "String rendering requires a tag-name builder. " +
                    "Use TagElement(tagName, ...) for custom elements."
            )
        val elementScope = remember { StringElementScope<TElement>() }

        ComposeStringNode(
            factory = {
                StringHtmlNodeWrapper(
                    StringHtmlElementNode(stringElementBuilder.tagName)
                )
            },
            update = {
                val attrsScope = AttrsScopeBuilder<TElement>()
                applyAttrs?.invoke(attrsScope)
                val attributes = attrsScope.stringAttributes()
                rawText?.validateAttributes(attributes)

                update {
                    set(attributes, StringHtmlNodeWrapper::updateAttributes)
                }
            },
            scope = elementScope,
            content = {
                content?.invoke(this)
            },
        )
    }

    @Composable
    override fun <TElement : Element> RawTextElement(
        tagName: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: RawTextContent,
    ) {
        StringTagElement(
            elementBuilder = elementBuilder(tagName),
            applyAttrs = applyAttrs,
            content = {
                StringRawText(content)
            },
            rawText = content,
        )
    }

    @Composable
    override fun TextElement(value: String) {
        ComposeStringNode(
            factory = {
                StringHtmlNodeWrapper(StringHtmlTextNode(value))
            },
            update = {
                update {
                    set(value, StringHtmlNodeWrapper::updateText)
                }
            },
            scope = Unit,
            content = {},
        )
    }

    @Composable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        val content = remember(cssRules, cssRules.size) {
            prepareStyleRawTextContent(cssRules)
        }
        RawTextElement<HTMLStyleElement>(
            tagName = "style",
            applyAttrs = applyAttrs,
            content = content,
        )
    }
}

@Composable
private fun StringRawText(content: RawTextContent) {
    ComposeStringNode(
        factory = {
            StringHtmlNodeWrapper(StringHtmlRawTextNode(content))
        },
        update = {
            update {
                set(content, StringHtmlNodeWrapper::updateRawText)
            }
        },
        scope = Unit,
        content = {},
    )
}

private class StringElementScope<TElement : Element> : ElementScopeBase<TElement>() {
    override val element: TElement
        get() = unavailableDomElement()

    override val DisposableEffectScope.scopeElement: TElement
        get() = unavailableDomElement()
}

private fun unavailableDomElement(): Nothing =
    throw UnsupportedOperationException(
        "DOM element references are not available during string rendering"
    )

/*
   Only reads: ordinary HTML attributes, classes, styleScope.properties, styleScope.variables
   Event listeners are intentionally ignored because they are client-side behavior, not string HTML behaviour.
   TODO not supported: attrsScope.refEffect, propertyUpdates

   ```kotlin
   Div({
    println("This does execute")
    onClick { println("This does not execute during rendering") }
   })
   ```
 */
@OptIn(ComposeWebInternalApi::class)
private fun <TElement : Element> AttrsScopeBuilder<TElement>.stringAttributes(): Map<String, String> =
    collect().toMutableMap().apply {
        if (AttrsScope.CLASS !in this && classes.isNotEmpty()) {
            classes.toClassAttributeValue()?.let { value -> this[AttrsScope.CLASS] = value }
        }

        if ("style" !in this) {
            styleScope.toStyleAttributeValue()?.let { value -> this["style"] = value }
        }
    }

@Composable
@ExplicitGroupsComposable
private inline fun <TScope> ComposeStringNode(
    crossinline factory: () -> StringHtmlNodeWrapper,
    update: @Composable SkippableUpdater<StringHtmlNodeWrapper>.() -> Unit,
    scope: TScope,
    content: @Composable TScope.() -> Unit,
) {
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode { factory() }
    } else {
        currentComposer.useNode()
    }

    update(SkippableUpdater(currentComposer))

    currentComposer.startReplaceableGroup(0x387e8a1c)
    content(scope)
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}
