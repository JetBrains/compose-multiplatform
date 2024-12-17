package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import androidx.compose.web.attributes.SelectAttrsScope
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.attributes.builders.*
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.StyleSheetBuilder
import org.jetbrains.compose.web.css.StyleSheetBuilderImpl
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomApplier
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLAreaElement
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.HTMLBRElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDataListElement
import org.w3c.dom.HTMLDListElement
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
import org.w3c.dom.Text
import org.w3c.dom.css.CSSStyleSheet

typealias AttrBuilderContext<T> = AttrsScope<T>.() -> Unit
typealias ContentBuilder<T> = @Composable ElementScope<T>.() -> Unit

private open class ElementBuilderImplementation<TElement : Element>(private val tagName: String) : ElementBuilder<TElement> {
    private val el: Element by lazy { document.createElement(tagName) }
    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement = el.cloneNode() as TElement
}

private val Address: ElementBuilder<HTMLElement> = ElementBuilderImplementation("address")
private val Article: ElementBuilder<HTMLElement> = ElementBuilderImplementation("article")
private val Aside: ElementBuilder<HTMLElement> = ElementBuilderImplementation("aside")
private val Header: ElementBuilder<HTMLElement> = ElementBuilderImplementation("header")

private val Area: ElementBuilder<HTMLAreaElement> = ElementBuilderImplementation("area")
private val Audio: ElementBuilder<HTMLAudioElement> = ElementBuilderImplementation("audio")
private val Map: ElementBuilder<HTMLMapElement> = ElementBuilderImplementation("map")
private val Track: ElementBuilder<HTMLTrackElement> = ElementBuilderImplementation("track")
private val Video: ElementBuilder<HTMLVideoElement> = ElementBuilderImplementation("video")

private val Datalist: ElementBuilder<HTMLDataListElement> = ElementBuilderImplementation("datalist")
private val Fieldset: ElementBuilder<HTMLFieldSetElement> = ElementBuilderImplementation("fieldset")
private val Legend: ElementBuilder<HTMLLegendElement> = ElementBuilderImplementation("legend")
private val Meter: ElementBuilder<HTMLMeterElement> = ElementBuilderImplementation("meter")
private val Output: ElementBuilder<HTMLOutputElement> = ElementBuilderImplementation("output")
private val Progress: ElementBuilder<HTMLProgressElement> = ElementBuilderImplementation("progress")

private val Embed: ElementBuilder<HTMLEmbedElement> = ElementBuilderImplementation("embed")
private val Iframe: ElementBuilder<HTMLIFrameElement> = ElementBuilderImplementation("iframe")
private val Object: ElementBuilder<HTMLObjectElement> = ElementBuilderImplementation("object")
private val Param: ElementBuilder<HTMLParamElement> = ElementBuilderImplementation("param")
private val Picture: ElementBuilder<HTMLPictureElement> = ElementBuilderImplementation("picture")
private val Source: ElementBuilder<HTMLSourceElement> = ElementBuilderImplementation("source")
private val Canvas: ElementBuilder<HTMLCanvasElement> = ElementBuilderImplementation("canvas")

private val DList: ElementBuilder<HTMLDListElement> = ElementBuilderImplementation("dl")
private val DTerm: ElementBuilder<HTMLElement> = ElementBuilderImplementation("dt")
private val DDescription: ElementBuilder<HTMLElement> = ElementBuilderImplementation("dd")

private val Div: ElementBuilder<HTMLDivElement> = ElementBuilderImplementation("div")
private val A: ElementBuilder<HTMLAnchorElement> = ElementBuilderImplementation("a")
private val Input: ElementBuilder<HTMLInputElement> = ElementBuilderImplementation("input")
private val Button: ElementBuilder<HTMLButtonElement> = ElementBuilderImplementation("button")

private val H1: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h1")
private val H2: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h2")
private val H3: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h3")
private val H4: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h4")
private val H5: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h5")
private val H6: ElementBuilder<HTMLHeadingElement> = ElementBuilderImplementation("h6")

private val P: ElementBuilder<HTMLParagraphElement> = ElementBuilderImplementation<HTMLParagraphElement>("p")

private val Em: ElementBuilder<HTMLElement> = ElementBuilderImplementation("em")
private val I: ElementBuilder<HTMLElement> = ElementBuilderImplementation("i")
private val B: ElementBuilder<HTMLElement> = ElementBuilderImplementation("b")
private val Small: ElementBuilder<HTMLElement> = ElementBuilderImplementation("small")
private val Sup: ElementBuilder<HTMLElement> = ElementBuilderImplementation("sup")
private val Sub: ElementBuilder<HTMLElement> = ElementBuilderImplementation("sub")
private val Blockquote: ElementBuilder<HTMLElement> = ElementBuilderImplementation("blockquote")

private val Span: ElementBuilder<HTMLSpanElement> = ElementBuilderImplementation("span")

private val Br: ElementBuilder<HTMLBRElement> = ElementBuilderImplementation("br")

private val Ul: ElementBuilder<HTMLUListElement> = ElementBuilderImplementation("ul")
private val Ol: ElementBuilder<HTMLOListElement> = ElementBuilderImplementation("ol")

private val Li: ElementBuilder<HTMLLIElement> = ElementBuilderImplementation("li")

private val Img: ElementBuilder<HTMLImageElement> = ElementBuilderImplementation("img")
private val Form: ElementBuilder<HTMLFormElement> = ElementBuilderImplementation("form")

private val Select: ElementBuilder<HTMLSelectElement> = ElementBuilderImplementation("select")
private val Option: ElementBuilder<HTMLOptionElement> = ElementBuilderImplementation("option")
private val OptGroup: ElementBuilder<HTMLOptGroupElement> = ElementBuilderImplementation("optgroup")

private val Section: ElementBuilder<HTMLElement> = ElementBuilderImplementation("section")
private val TextArea: ElementBuilder<HTMLTextAreaElement> = ElementBuilderImplementation("textarea")
private val Nav: ElementBuilder<HTMLElement> = ElementBuilderImplementation("nav")
private val Pre: ElementBuilder<HTMLPreElement> = ElementBuilderImplementation("pre")
private val Code: ElementBuilder<HTMLElement> = ElementBuilderImplementation("code")

private val Main: ElementBuilder<HTMLElement> = ElementBuilderImplementation("main")
private val Footer: ElementBuilder<HTMLElement> = ElementBuilderImplementation("footer")
private val Hr: ElementBuilder<HTMLHRElement> = ElementBuilderImplementation("hr")
private val Label: ElementBuilder<HTMLLabelElement> = ElementBuilderImplementation("label")
private val Table: ElementBuilder<HTMLTableElement> = ElementBuilderImplementation("table")
private val Caption: ElementBuilder<HTMLTableCaptionElement> = ElementBuilderImplementation("caption")
private val Col: ElementBuilder<HTMLTableColElement> = ElementBuilderImplementation("col")
private val Colgroup: ElementBuilder<HTMLTableColElement> = ElementBuilderImplementation("colgroup")
private val Tr: ElementBuilder<HTMLTableRowElement> = ElementBuilderImplementation("tr")
private val Thead: ElementBuilder<HTMLTableSectionElement> = ElementBuilderImplementation("thead")
private val Th: ElementBuilder<HTMLTableCellElement> = ElementBuilderImplementation("th")
private val Td: ElementBuilder<HTMLTableCellElement> = ElementBuilderImplementation("td")
private val Tbody: ElementBuilder<HTMLTableSectionElement> = ElementBuilderImplementation("tbody")
private val Tfoot: ElementBuilder<HTMLTableSectionElement> = ElementBuilderImplementation("tfoot")

internal val Style: ElementBuilder<HTMLStyleElement> = ElementBuilderImplementation("style")

fun interface ElementBuilder<TElement : Element> {
    fun create(): TElement

    companion object {
        // it's internal only for testing purposes
        internal val buildersCache = mutableMapOf<String, ElementBuilder<*>>()

        fun <TElement : Element> createBuilder(tagName: String): ElementBuilder<TElement> {
            val tagLowercase = tagName.lowercase()
            return buildersCache.getOrPut(tagLowercase) {
                ElementBuilderImplementation<TElement>(tagLowercase)
            }.unsafeCast<ElementBuilder<TElement>>()
        }
    }
}

@Composable
fun Address(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = Address,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Article(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = Article,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Aside(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = Aside,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Header(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = Header,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Area(
    attrs: AttrBuilderContext<HTMLAreaElement>? = null,
    content: ContentBuilder<HTMLAreaElement>? = null
) {
    TagElement(
        elementBuilder = Area,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Audio(
    attrs: AttrBuilderContext<HTMLAudioElement>? = null,
    content: ContentBuilder<HTMLAudioElement>? = null
) {
    TagElement(
        elementBuilder = Audio,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun HTMLMap(
    attrs: AttrBuilderContext<HTMLMapElement>? = null,
    content: ContentBuilder<HTMLMapElement>? = null
) {
    TagElement(
        elementBuilder = Map,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Track(
    attrs: AttrBuilderContext<HTMLTrackElement>? = null,
    content: ContentBuilder<HTMLTrackElement>? = null
) {
    TagElement(
        elementBuilder = Track,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Video(
    attrs: AttrBuilderContext<HTMLVideoElement>? = null,
    content: ContentBuilder<HTMLVideoElement>? = null
) {
    TagElement(
        elementBuilder = Video,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Datalist(
    attrs: AttrBuilderContext<HTMLDataListElement>? = null,
    content: ContentBuilder<HTMLDataListElement>? = null
) {
    TagElement(
        elementBuilder = Datalist,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Fieldset(
    attrs: AttrBuilderContext<HTMLFieldSetElement>? = null,
    content: ContentBuilder<HTMLFieldSetElement>? = null
) {
    TagElement(
        elementBuilder = Fieldset,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Legend(
    attrs: AttrBuilderContext<HTMLLegendElement>? = null,
    content: ContentBuilder<HTMLLegendElement>? = null
) {
    TagElement(
        elementBuilder = Legend,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Meter(
    attrs: AttrBuilderContext<HTMLMeterElement>? = null,
    content: ContentBuilder<HTMLMeterElement>? = null
) {
    TagElement(
        elementBuilder = Meter,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Output(
    attrs: AttrBuilderContext<HTMLOutputElement>? = null,
    content: ContentBuilder<HTMLOutputElement>? = null
) {
    TagElement(
        elementBuilder = Output,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Progress(
    attrs: AttrBuilderContext<HTMLProgressElement>? = null,
    content: ContentBuilder<HTMLProgressElement>? = null
) {
    TagElement(
        elementBuilder = Progress,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Embed(
    attrs: AttrBuilderContext<HTMLEmbedElement>? = null,
    content: ContentBuilder<HTMLEmbedElement>? = null
) {
    TagElement(
        elementBuilder = Embed,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Iframe(
    attrs: AttrBuilderContext<HTMLIFrameElement>? = null,
    content: ContentBuilder<HTMLIFrameElement>? = null
) {
    TagElement(
        elementBuilder = Iframe,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Object(
    attrs: AttrBuilderContext<HTMLObjectElement>? = null,
    content: ContentBuilder<HTMLObjectElement>? = null
) {
    TagElement(
        elementBuilder = Object,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Param(
    attrs: AttrBuilderContext<HTMLParamElement>? = null,
    content: ContentBuilder<HTMLParamElement>? = null
) {
    TagElement(
        elementBuilder = Param,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Picture(
    attrs: AttrBuilderContext<HTMLPictureElement>? = null,
    content: ContentBuilder<HTMLPictureElement>? = null
) {
    TagElement(
        elementBuilder = Picture,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Source(
    attrs: AttrBuilderContext<HTMLSourceElement>? = null,
    content: ContentBuilder<HTMLSourceElement>? = null
) {
    TagElement(
        elementBuilder = Source,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Canvas(
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null
) {
    TagElement(
        elementBuilder = Canvas,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun DList(
    attrs: AttrBuilderContext<HTMLDListElement>? = null,
    content: ContentBuilder<HTMLDListElement>? = null
) {
    TagElement(
        elementBuilder = DList,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun DDescription(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DDescription,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun DTerm(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DTerm,
        applyAttrs = attrs,
        content = content
    )
}

@OptIn(ComposeWebInternalApi::class)
@Composable
fun Text(value: String) {
    ComposeNode<DomNodeWrapper, DomApplier>(
        factory = { DomNodeWrapper(document.createTextNode("")) },
        update = {
            set(value) { value -> (node as Text).data = value }
        },
    )
}

@Composable
fun Div(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    TagElement(
        elementBuilder = Div,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun A(
    href: String? = null,
    attrs: AttrBuilderContext<HTMLAnchorElement>? = null,
    content: ContentBuilder<HTMLAnchorElement>? = null
) {
    TagElement(
        elementBuilder = A,
        applyAttrs = {
            if (href != null) {
                this.href(href)
            }
            if (attrs != null) {
                attrs()
            }
        },
        content = content
    )
}

@Composable
fun Button(
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
    content: ContentBuilder<HTMLButtonElement>? = null
) = TagElement(elementBuilder = Button, applyAttrs = attrs, content = content)

@Composable
fun H1(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = H1, applyAttrs = attrs, content = content)

@Composable
fun H2(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = H2, applyAttrs = attrs, content = content)

@Composable
fun H3(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = H3, applyAttrs = attrs, content = content)

@Composable
fun H4(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = H4, applyAttrs = attrs, content = content)

@Composable
fun H5(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = H5, applyAttrs = attrs, content = content)

@Composable
fun H6(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = H6, applyAttrs = attrs, content = content)

@Composable
fun P(
    attrs: AttrBuilderContext<HTMLParagraphElement>? = null,
    content: ContentBuilder<HTMLParagraphElement>? = null
) = TagElement(elementBuilder = P, applyAttrs = attrs, content = content)

@Composable
fun Em(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = Em, applyAttrs = attrs, content = content)

@Composable
fun I(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = I, applyAttrs = attrs, content = content)

@Composable
fun B(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = B, applyAttrs = attrs, content = content)

@Composable
fun Small(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = Small, applyAttrs = attrs, content = content)

@Composable
fun Sup(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = Sup, applyAttrs = attrs, content = content)

@Composable
fun Sub(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = Sub, applyAttrs = attrs, content = content)

@Composable
fun Blockquote(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = Blockquote, applyAttrs = attrs, content = content)

@Composable
fun Span(
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
    content: ContentBuilder<HTMLSpanElement>? = null
) = TagElement(elementBuilder = Span, applyAttrs = attrs, content = content)

@Composable
fun Br(attrs: AttrBuilderContext<HTMLBRElement>? = null) =
    TagElement(elementBuilder = Br, applyAttrs = attrs, content = null)

@Composable
fun Ul(
    attrs: AttrBuilderContext<HTMLUListElement>? = null,
    content: ContentBuilder<HTMLUListElement>? = null
) = TagElement(elementBuilder = Ul, applyAttrs = attrs, content = content)

@Composable
fun Ol(
    attrs: AttrBuilderContext<HTMLOListElement>? = null,
    content: ContentBuilder<HTMLOListElement>? = null
) = TagElement(elementBuilder = Ol, applyAttrs = attrs, content = content)

@Composable
fun Li(
    attrs: AttrBuilderContext<HTMLLIElement>? = null,
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = Li, applyAttrs = attrs, content = content)

@Composable
fun Img(
    src: String,
    alt: String = "",
    attrs: AttrBuilderContext<HTMLImageElement>? = null
) = TagElement(
    elementBuilder = Img,
    applyAttrs = {
        src(src).alt(alt)
        if (attrs != null) {
            attrs()
        }
    },
    content = null
)

@Composable
fun Form(
    action: String? = null,
    attrs: AttrBuilderContext<HTMLFormElement>? = null,
    content: ContentBuilder<HTMLFormElement>? = null
) = TagElement(
    elementBuilder = Form,
    applyAttrs = {
        if (!action.isNullOrEmpty()) action(action)
        if (attrs != null) {
            attrs()
        }
    },
    content = content
)

@Composable
fun Select(
    attrs: (SelectAttrsScope.() -> Unit)? = null,
    multiple: Boolean = false,
    content: ContentBuilder<HTMLSelectElement>? = null
) = TagElement(
    elementBuilder = Select,
    applyAttrs = {
        if (multiple) multiple()
        if (attrs != null) {
            SelectAttrsScope(this).attrs()
        }
    },
    content = content
)

@Composable
fun Option(
    value: String,
    attrs: AttrBuilderContext<HTMLOptionElement>? = null,
    content: ContentBuilder<HTMLOptionElement>? = null
) = TagElement(
    elementBuilder = Option,
    applyAttrs = {
        value(value)
        if (attrs != null) {
            attrs()
        }
    },
    content = content
)

@Composable
fun OptGroup(
    label: String,
    attrs: AttrBuilderContext<HTMLOptGroupElement>? = null,
    content: ContentBuilder<HTMLOptGroupElement>? = null
) = TagElement(
    elementBuilder = OptGroup,
    applyAttrs = {
        label(label)
        if (attrs != null) {
            attrs()
        }
    },
    content = content
)

@Composable
fun Section(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(
    elementBuilder = Section,
    applyAttrs = attrs,
    content = content
)

/**
 * Adds <textarea> element.
 * Same as [Input], [TextArea] has two modes: controlled and uncontrolled.
 *
 * Controlled mode means that <textarea> value can be changed only by passing a different [value].
 * Uncontrolled mode means that <textarea> uses its default state management.
 *
 * To use controlled mode, simply pass non-null [value].
 * By default [value] is null and [TextArea] will be in uncontrolled mode.
 *
 * Use `defaultValue("some default text")` in uncontrolled mode to set a default text if needed:
 *
 * ```
 * TextArea {
 *      defaultValue("Some Default Text")
 * }
 * ```
 */
@Composable
fun TextArea(
    value: String? = null,
    attrs: (TextAreaAttrsScope.() -> Unit)? = null
) {
    // if firstProvidedValueWasNotNull then TextArea behaves as controlled input
    val firstProvidedValueWasNotNull = remember { value != null }

    // changes to this key trigger [textAreaRestoreControlledStateEffect]
    val keyForRestoringControlledState: MutableState<Int> = remember { mutableStateOf(0) }

    TagElement(
        elementBuilder = TextArea,
        applyAttrs = {
            val textAreaAttrsBuilder = TextAreaAttrsScope(this)
            textAreaAttrsBuilder.onInput {
                // controlled state needs to be restored after every input
                keyForRestoringControlledState.value = keyForRestoringControlledState.value + 1
            }
            if (attrs != null) {
                textAreaAttrsBuilder.attrs()
            }
            if (firstProvidedValueWasNotNull) {
                textAreaAttrsBuilder.value(value ?: "")
            }
        },
        content = {
            DisposableEffect(keyForRestoringControlledState.value) {
                restoreControlledTextAreaState(element = scopeElement)
                onDispose { }
            }
        }
    )
}

@Composable
fun Nav(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(
    elementBuilder = Nav,
    applyAttrs = attrs,
    content = content
)

@Composable
fun Pre(
    attrs: AttrBuilderContext<HTMLPreElement>? = null,
    content: ContentBuilder<HTMLPreElement>? = null
) {
    TagElement(
        elementBuilder = Pre,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Code(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = Code,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Main(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = Main,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Footer(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = Footer,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Hr(
    attrs: AttrBuilderContext<HTMLHRElement>? = null
) {
    TagElement(
        elementBuilder = Hr,
        applyAttrs = attrs,
        content = null
    )
}

@Composable
fun Label(
    forId: String? = null,
    attrs: AttrBuilderContext<HTMLLabelElement>? = null,
    content: ContentBuilder<HTMLLabelElement>? = null
) {
    TagElement(
        elementBuilder = Label,
        applyAttrs = {
            if (forId != null) {
                forId(forId)
            }
            if (attrs != null) {
                attrs()
            }
        },
        content = content
    )
}

@Composable
fun Table(
    attrs: AttrBuilderContext<HTMLTableElement>? = null,
    content: ContentBuilder<HTMLTableElement>? = null
) {
    TagElement(
        elementBuilder = Table,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Caption(
    attrs: AttrBuilderContext<HTMLTableCaptionElement>? = null,
    content: ContentBuilder<HTMLTableCaptionElement>? = null
) {
    TagElement(
        elementBuilder = Caption,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Col(
    attrs: AttrBuilderContext<HTMLTableColElement>? = null
) {
    TagElement(
        elementBuilder = Col,
        applyAttrs = attrs,
        content = null
    )
}

@Composable
fun Colgroup(
    attrs: AttrBuilderContext<HTMLTableColElement>? = null,
    content: ContentBuilder<HTMLTableColElement>? = null
) {
    TagElement(
        elementBuilder = Colgroup,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tr(
    attrs: AttrBuilderContext<HTMLTableRowElement>? = null,
    content: ContentBuilder<HTMLTableRowElement>? = null
) {
    TagElement(
        elementBuilder = Tr,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Thead(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = Thead,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Th(
    attrs: AttrBuilderContext<HTMLTableCellElement>? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null
) {
    TagElement(
        elementBuilder = Th,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Td(
    attrs: AttrBuilderContext<HTMLTableCellElement>? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null
) {
    TagElement(
        elementBuilder = Td,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tbody(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = Tbody,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tfoot(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = Tfoot,
        applyAttrs = attrs,
        content = content
    )
}

/**
 * Use this function to mount the <style> tag into the DOM tree.
 *
 * @param cssRules - is a list of style rules.
 * Usually, it's [androidx.compose.web.css.StyleSheet] instance
 */
@Composable
fun Style(
    applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)? = null,
    cssRules: CSSRuleDeclarationList
) {
    TagElement(
        elementBuilder = Style,
        applyAttrs = {
            if (applyAttrs != null) {
                applyAttrs()
            }
        },
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

/**
 * Use this function to mount the <style> tag into the DOM tree.
 *
 * @param rulesBuild allows to define the style rules using [StyleSheetBuilder]
 */
@Composable
inline fun Style(
    noinline applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)? = null,
    rulesBuild: StyleSheetBuilder.() -> Unit
) {
    val builder = StyleSheetBuilderImpl()
    builder.rulesBuild()
    Style(applyAttrs, builder.cssRules)
}

private fun CSSStyleSheet.clearCSSRules() {
    repeat(cssRules.length) {
        deleteRule(0)
    }
}

/**
 * Adds <input> element of [type].
 *
 * Input has two modes: controlled and uncontrolled.
 * Uncontrolled is a default mode. The input's state is managed by [HTMLInputElement] itself.
 * Controlled mode means that the input's state is managed by compose state.
 * To use Input in controlled mode, it's required to set its state by calling `value(String|Number)`.
 *
 * Consider using [TextInput], [CheckboxInput], [RadioInput], [NumberInput] etc. to use controlled mode.
 *
 * Code example of a controlled Input:
 * ```
 * val textInputState by remember { mutableStateOf("initial text") }
 *
 * Input(type = InputType.Text) {
 *      value(textInputState)
 *      onInput { event ->
 *          textInputState = event.value // without updating the state, the <input> will keep showing an old value
 *      }
 * }
 * ```
 *
 * Code example of an uncontrolled Input:
 * ```
 * Input(type = InputType.Text) {
 *      defaultValue("someDefaultValue") // calling `defaultValue` is optional
 *      // No value set explicitly.
 *      // Whatever typed into the input will be immediately displayed in UI without handling any onInput events.
 * }
 * ```
 */
@OptIn(ComposeWebInternalApi::class)
@Composable
fun <K> Input(
    type: InputType<K>,
    attrs: InputAttrsScope<K>.() -> Unit
) {
    // changes to this key trigger [inputRestoreControlledStateEffect]
    val keyForRestoringControlledState: MutableState<Int> = remember { mutableStateOf(0) }

    TagElement(
        elementBuilder = Input,
        applyAttrs = {
            val inputAttrsBuilder = InputAttrsScope(type, this)
            inputAttrsBuilder.type(type)
            inputAttrsBuilder.onInput {
                // controlled state needs to be restored after every input
                keyForRestoringControlledState.value = keyForRestoringControlledState.value + 1
            }

            inputAttrsBuilder.attrs()
        },
        content = {
            if (type == InputType.Radio) {
                DisposeRadioGroupEffect()
            }
            DisposableEffect(keyForRestoringControlledState.value) {
                restoreControlledInputState(inputElement = scopeElement)
                onDispose { }
            }
        }
    )
}

@Composable
fun <K> Input(type: InputType<K>) {
    Input(type) {}
}
