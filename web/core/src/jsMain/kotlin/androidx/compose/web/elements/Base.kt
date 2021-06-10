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

class ElementBuilder<THTMLElement : HTMLElement>(private val tagName: String) {
    private val el: Element by lazy { document.createElement(tagName) }
    fun create(): THTMLElement = el.cloneNode() as THTMLElement

    companion object {
        val Div = ElementBuilder<HTMLDivElement>("div")
        val A = ElementBuilder<HTMLAnchorElement>("a")
        val Input = ElementBuilder<HTMLInputElement>("input")
        val Button = ElementBuilder<HTMLButtonElement>("button")

        val H1 = ElementBuilder<HTMLHeadingElement>("h1")
        val H2 = ElementBuilder<HTMLHeadingElement>("h2")
        val H3 = ElementBuilder<HTMLHeadingElement>("h3")
        val H4 = ElementBuilder<HTMLHeadingElement>("h4")
        val H5 = ElementBuilder<HTMLHeadingElement>("h5")
        val H6 = ElementBuilder<HTMLHeadingElement>("h6")

        val P = ElementBuilder<HTMLParagraphElement>("p")

        val Em = ElementBuilder<HTMLElement>("em")
        val I = ElementBuilder<HTMLElement>("i")
        val B = ElementBuilder<HTMLElement>("b")
        val Small = ElementBuilder<HTMLElement>("small")

        val Span = ElementBuilder<HTMLSpanElement>("span")

        val Br = ElementBuilder<HTMLBRElement>("br")

        val Ul = ElementBuilder<HTMLUListElement>("ul")
        val Ol = ElementBuilder<HTMLOListElement>("ol")

        val Li = ElementBuilder<HTMLLIElement>("li")

        val Img = ElementBuilder<HTMLImageElement>("img")
        val Form = ElementBuilder<HTMLFormElement>("form")

        val Select = ElementBuilder<HTMLSelectElement>("select")
        val Option = ElementBuilder<HTMLOptionElement>("option")
        val OptGroup = ElementBuilder<HTMLOptGroupElement>("optgroup")

        val Section = ElementBuilder<HTMLElement>("section")
        val TextArea = ElementBuilder<HTMLTextAreaElement>("textarea")
        val Nav = ElementBuilder<HTMLElement>("nav")
        val Pre = ElementBuilder<HTMLPreElement>("pre")
        val Code = ElementBuilder<HTMLElement>("code")

        val Main = ElementBuilder<HTMLElement>("main")
        val Footer = ElementBuilder<HTMLElement>("footer")
        val Hr = ElementBuilder<HTMLHRElement>("hr")
        val Label = ElementBuilder<HTMLElement>("label")
        val Table = ElementBuilder<HTMLTableElement>("table")
        val Caption = ElementBuilder<HTMLTableCaptionElement>("caption")
        val Col = ElementBuilder<HTMLTableColElement>("col")
        val Colgroup = ElementBuilder<HTMLTableColElement>("colgroup")
        val Tr = ElementBuilder<HTMLTableRowElement>("tr")
        val Thead = ElementBuilder<HTMLTableSectionElement>("thead")
        val Th = ElementBuilder<HTMLTableCellElement>("th")
        val Td = ElementBuilder<HTMLTableCellElement>("td")
        val Tbody = ElementBuilder<HTMLTableSectionElement>("tbody")
        val Tfoot = ElementBuilder<HTMLTableSectionElement>("tfoot")
    }
}

@Composable
fun <TTag : Tag, TElement : Element> TagElement(
    elementBuilder: () -> HTMLElement,
    applyAttrs: AttrsBuilder<TTag>.() -> Unit,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    val scope = remember { ElementScopeImpl<TElement>() }
    val refEffect = remember { DisposableEffectHolder() }

    ComposeDomNode<ElementScope<TElement>, DomElementWrapper, DomApplier>(
        factory = {
            DomElementWrapper(elementBuilder()).also {
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
)  = TagElement(
    elementBuilder = {  document.createElement(tagName) as HTMLElement },
    applyAttrs = applyAttrs,
    content = content
)
