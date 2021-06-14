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
import kotlinx.browser.document
import org.w3c.dom.Audio
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLAreaElement
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.HTMLBRElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDataListElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLEmbedElement
import org.w3c.dom.HTMLFieldSetElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLHRElement
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLIElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.HTMLLegendElement
import org.w3c.dom.HTMLMapElement
import org.w3c.dom.HTMLMeterElement
import org.w3c.dom.HTMLOListElement
import org.w3c.dom.HTMLObjectElement
import org.w3c.dom.HTMLOptGroupElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLOutputElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLParamElement
import org.w3c.dom.HTMLPictureElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.HTMLProgressElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLSourceElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTableCaptionElement
import org.w3c.dom.HTMLTableCellElement
import org.w3c.dom.HTMLTableColElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.HTMLTableSectionElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.HTMLTrackElement
import org.w3c.dom.HTMLUListElement
import org.w3c.dom.HTMLVideoElement

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

    private open class ElementBuilderImplementation<TElement : Element>(private val tagName: String) : ElementBuilder<TElement> {
        private val el = document.createElement(tagName)
        override fun create(): TElement = el.cloneNode() as TElement
    }

    companion object {
        fun <TElement : Element> createBuilder(tagName: String): ElementBuilder<TElement> {
            return object  : ElementBuilderImplementation<TElement>(tagName) {}
        }

        val Address: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("address") }
        val Article: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("article") }
        val Aside: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("aside") }
        val Header: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("header") }

        val Area: ElementBuilder<HTMLAreaElement> by lazy { ElementBuilderImplementation("area") }
        val Audio: ElementBuilder<HTMLAudioElement> by lazy { ElementBuilderImplementation("audio") }
        val Map: ElementBuilder<HTMLMapElement> by lazy { ElementBuilderImplementation("map") }
        val Track: ElementBuilder<HTMLTrackElement> by lazy { ElementBuilderImplementation("track") }
        val Video: ElementBuilder<HTMLVideoElement> by lazy { ElementBuilderImplementation("video") }

        val Datalist: ElementBuilder<HTMLDataListElement> by lazy { ElementBuilderImplementation("datalist") }
        val Fieldset: ElementBuilder<HTMLFieldSetElement> by lazy { ElementBuilderImplementation("fieldset") }
        val Legend: ElementBuilder<HTMLLegendElement> by lazy { ElementBuilderImplementation("legend") }
        val Meter: ElementBuilder<HTMLMeterElement> by lazy { ElementBuilderImplementation("meter") }
        val Output: ElementBuilder<HTMLOutputElement> by lazy { ElementBuilderImplementation("output") }
        val Progress: ElementBuilder<HTMLProgressElement> by lazy { ElementBuilderImplementation("progress") }

        val Embed: ElementBuilder<HTMLEmbedElement> by lazy { ElementBuilderImplementation("embed") }
        val Iframe: ElementBuilder<HTMLIFrameElement> by lazy { ElementBuilderImplementation("iframe") }
        val Object: ElementBuilder<HTMLObjectElement> by lazy { ElementBuilderImplementation("object") }
        val Param: ElementBuilder<HTMLParamElement> by lazy { ElementBuilderImplementation("param") }
        val Picture: ElementBuilder<HTMLPictureElement> by lazy { ElementBuilderImplementation("picture") }
        val Source: ElementBuilder<HTMLSourceElement> by lazy { ElementBuilderImplementation("source") }

        val Div: ElementBuilder<HTMLDivElement> by lazy { ElementBuilderImplementation("div") }
        val A: ElementBuilder<HTMLAnchorElement> by lazy { ElementBuilderImplementation("a") }
        val Input: ElementBuilder<HTMLInputElement> by lazy { ElementBuilderImplementation("input") }
        val Button: ElementBuilder<HTMLButtonElement> by lazy { ElementBuilderImplementation("button") }

        val H1: ElementBuilder<HTMLHeadingElement> by lazy { ElementBuilderImplementation("h1") }
        val H2: ElementBuilder<HTMLHeadingElement> by lazy { ElementBuilderImplementation("h2") }
        val H3: ElementBuilder<HTMLHeadingElement> by lazy { ElementBuilderImplementation("h3") }
        val H4: ElementBuilder<HTMLHeadingElement> by lazy { ElementBuilderImplementation("h4") }
        val H5: ElementBuilder<HTMLHeadingElement> by lazy { ElementBuilderImplementation("h5") }
        val H6: ElementBuilder<HTMLHeadingElement> by lazy { ElementBuilderImplementation("h6") }

        val P: ElementBuilder<HTMLParagraphElement> by lazy { ElementBuilderImplementation<HTMLParagraphElement>("p") }

        val Em: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("em") }
        val I: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("i") }
        val B: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("b") }
        val Small: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("small") }

        val Span: ElementBuilder<HTMLSpanElement> by lazy { ElementBuilderImplementation("span") }

        val Br: ElementBuilder<HTMLBRElement> by lazy { ElementBuilderImplementation("br") }

        val Ul: ElementBuilder<HTMLUListElement> by lazy { ElementBuilderImplementation("ul") }
        val Ol: ElementBuilder<HTMLOListElement> by lazy { ElementBuilderImplementation("ol") }

        val Li: ElementBuilder<HTMLLIElement> by lazy { ElementBuilderImplementation("li") }

        val Img: ElementBuilder<HTMLImageElement> by lazy { ElementBuilderImplementation("img") }
        val Form: ElementBuilder<HTMLFormElement> by lazy { ElementBuilderImplementation("form") }

        val Select: ElementBuilder<HTMLSelectElement> by lazy { ElementBuilderImplementation("select") }
        val Option: ElementBuilder<HTMLOptionElement> by lazy { ElementBuilderImplementation("option") }
        val OptGroup: ElementBuilder<HTMLOptGroupElement> by lazy { ElementBuilderImplementation("optgroup") }

        val Section: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("section") }
        val TextArea: ElementBuilder<HTMLTextAreaElement> by lazy { ElementBuilderImplementation("textarea") }
        val Nav: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("nav") }
        val Pre: ElementBuilder<HTMLPreElement> by lazy { ElementBuilderImplementation("pre") }
        val Code: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("code") }

        val Main: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("main") }
        val Footer: ElementBuilder<HTMLElement> by lazy { ElementBuilderImplementation("footer") }
        val Hr: ElementBuilder<HTMLHRElement> by lazy { ElementBuilderImplementation("hr") }
        val Label: ElementBuilder<HTMLLabelElement> by lazy { ElementBuilderImplementation("label") }
        val Table: ElementBuilder<HTMLTableElement> by lazy { ElementBuilderImplementation("table") }
        val Caption: ElementBuilder<HTMLTableCaptionElement> by lazy { ElementBuilderImplementation("caption") }
        val Col: ElementBuilder<HTMLTableColElement> by lazy { ElementBuilderImplementation("col") }
        val Colgroup: ElementBuilder<HTMLTableColElement> by lazy { ElementBuilderImplementation("colgroup") }
        val Tr: ElementBuilder<HTMLTableRowElement> by lazy { ElementBuilderImplementation("tr") }
        val Thead: ElementBuilder<HTMLTableSectionElement> by lazy { ElementBuilderImplementation("thead") }
        val Th: ElementBuilder<HTMLTableCellElement> by lazy { ElementBuilderImplementation("th") }
        val Td: ElementBuilder<HTMLTableCellElement> by lazy { ElementBuilderImplementation("td") }
        val Tbody: ElementBuilder<HTMLTableSectionElement> by lazy { ElementBuilderImplementation("tbody") }
        val Tfoot: ElementBuilder<HTMLTableSectionElement> by lazy { ElementBuilderImplementation("tfoot") }

        val Style: ElementBuilder<HTMLStyleElement> by lazy { ElementBuilderImplementation("style") }
    }
}

@Composable
fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: AttrsBuilder<TElement>.() -> Unit,
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
            val attrsApplied = AttrsBuilder<TElement>().also { it.applyAttrs() }
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
fun <TElement : Element> TagElement(
    tagName: String,
    applyAttrs: AttrsBuilder<TElement>.() -> Unit,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) = TagElement(
    elementBuilder = ElementBuilder.createBuilder(tagName),
    applyAttrs = applyAttrs,
    content = content
)