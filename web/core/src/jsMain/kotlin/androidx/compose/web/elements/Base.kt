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
import org.jetbrains.compose.web.DomApplier
import org.jetbrains.compose.web.DomElementWrapper
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.Tag
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLBRElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLHRElement
import org.w3c.dom.HTMLHeadElement
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLIElement
import org.w3c.dom.HTMLOListElement
import org.w3c.dom.HTMLOptGroupElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTableCaptionElement
import org.w3c.dom.HTMLTableCellElement
import org.w3c.dom.HTMLTableColElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.HTMLTableSectionElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.HTMLUListElement

@OptIn(ComposeCompilerApi::class)
@Composable
@ExplicitGroupsComposable
inline fun <TScope, T, reified E : Applier<*>> ComposeDomNode(
    noinline factory: () -> T,
    elementScope: TScope,
    noinline attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    noinline content: (@Composable TScope.() -> Unit)?
) {
    if (currentComposer.applier !is E) error("Invalid applier")
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

class DisposableEffectHolder(
    var effect: (DisposableEffectScope.(Element) -> DisposableEffectResult)? = null
)

interface ElementBuilder<TElement : Element> {
    fun create(): TElement

    private open class ElementBuilderImplementation<TElement : HTMLElement>(private val tagName: String) : ElementBuilder<TElement> {
        private val el: Element by lazy { document.createElement(tagName) }
        override fun create(): TElement = el.cloneNode() as TElement
    }

    companion object {
        fun <TElement : HTMLElement> createBuilder(tagName: String): ElementBuilder<TElement> {
            return object  : ElementBuilderImplementation<TElement>(tagName) {}
        }

        val Div: ElementBuilder<HTMLDivElement> = ElementBuilderImplementation("div")
        val A: ElementBuilder<HTMLAnchorElement> = ElementBuilderImplementation("a")
        val Input: ElementBuilder<HTMLInputElement> = ElementBuilderImplementation("input")
        val Button: ElementBuilder<HTMLButtonElement> = ElementBuilderImplementation("button")

        val H1: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h1")
        val H2: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h2")
        val H3: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h3")
        val H4: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h4")
        val H5: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h5")
        val H6: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h6")

        val P: ElementBuilder<HTMLParagraphElement> = ElementBuilderImplementation<HTMLParagraphElement>("p")

        val Em: ElementBuilder<HTMLElement> = ElementBuilderImplementation("em")
        val I: ElementBuilder<HTMLElement> = ElementBuilderImplementation("i")
        val B: ElementBuilder<HTMLElement> = ElementBuilderImplementation("b")
        val Small: ElementBuilder<HTMLElement> = ElementBuilderImplementation("small")

        val Span: ElementBuilder<HTMLSpanElement> = ElementBuilderImplementation("span")

        val Br: ElementBuilder<HTMLBRElement> = ElementBuilderImplementation("br")

        val Ul: ElementBuilder<HTMLUListElement> = ElementBuilderImplementation("ul")
        val Ol: ElementBuilder<HTMLOListElement> = ElementBuilderImplementation("ol")

        val Li: ElementBuilder<HTMLLIElement> = ElementBuilderImplementation("li")

        val Img: ElementBuilder<HTMLImageElement> = ElementBuilderImplementation("img")
        val Form: ElementBuilder<HTMLFormElement> = ElementBuilderImplementation("form")

        val Select: ElementBuilder<HTMLSelectElement> = ElementBuilderImplementation("select")
        val Option: ElementBuilder<HTMLOptionElement> = ElementBuilderImplementation("option")
        val OptGroup: ElementBuilder<HTMLOptGroupElement> = ElementBuilderImplementation("optgroup")

        val Section: ElementBuilder<HTMLElement> = ElementBuilderImplementation("section")
        val TextArea: ElementBuilder<HTMLTextAreaElement> = ElementBuilderImplementation("textarea")
        val Nav: ElementBuilder<HTMLElement> = ElementBuilderImplementation("nav")
        val Pre: ElementBuilder<HTMLPreElement> = ElementBuilderImplementation("pre")
        val Code: ElementBuilder<HTMLElement> = ElementBuilderImplementation("code")

        val Main: ElementBuilder<HTMLElement> = ElementBuilderImplementation("main")
        val Footer: ElementBuilder<HTMLElement> = ElementBuilderImplementation("footer")
        val Hr: ElementBuilder<HTMLHRElement> = ElementBuilderImplementation("hr")
        val Label: ElementBuilder<HTMLElement> = ElementBuilderImplementation("label")
        val Table: ElementBuilder<HTMLTableElement> = ElementBuilderImplementation("table")
        val Caption: ElementBuilder<HTMLTableCaptionElement> = ElementBuilderImplementation("caption")
        val Col: ElementBuilder<HTMLTableColElement> = ElementBuilderImplementation("col")
        val Colgroup: ElementBuilder<HTMLTableColElement> = ElementBuilderImplementation("colgroup")
        val Tr: ElementBuilder<HTMLTableRowElement> = ElementBuilderImplementation("tr")
        val Thead: ElementBuilder<HTMLTableSectionElement> = ElementBuilderImplementation("thead")
        val Th: ElementBuilder<HTMLTableCellElement> = ElementBuilderImplementation("th")
        val Td: ElementBuilder<HTMLTableCellElement> = ElementBuilderImplementation("td")
        val Tbody: ElementBuilder<HTMLTableSectionElement> = ElementBuilderImplementation("tbody")
        val Tfoot: ElementBuilder<HTMLTableSectionElement> = ElementBuilderImplementation("tfoot")

        val Style: ElementBuilder<HTMLStyleElement> = ElementBuilderImplementation("style")
    }
}

@Composable
fun <TTag : Tag, TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: AttrsBuilder<TTag>.() -> Unit,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    val scope = remember { ElementScopeImpl<TElement>() }
    val refEffect = remember { DisposableEffectHolder() }

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper, DomApplier>(
        factory = {
            DomElementWrapper(elementBuilder.create() as HTMLElement).also {
                scope.element = it.node.unsafeCast<TElement>()
            }
        },
        attrsSkippableUpdate = {
            val attrsApplied = AttrsBuilder<TTag>().also { it.applyAttrs() }
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

    DisposableEffect(null) {
        refEffect.effect?.invoke(this, scope.element) ?: onDispose {}
    }
}

@Composable
fun <TTag : Tag, TElement : Element> TagElement(
    tagName: String,
    applyAttrs: AttrsBuilder<TTag>.() -> Unit,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) = TagElement(
    elementBuilder = ElementBuilder.createBuilder(tagName),
    applyAttrs = applyAttrs,
    content = content
)