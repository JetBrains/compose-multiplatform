package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.jetbrains.compose.web.DomApplier
import org.jetbrains.compose.web.DomNodeWrapper
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.action
import org.jetbrains.compose.web.attributes.alt
import org.jetbrains.compose.web.attributes.forId
import org.jetbrains.compose.web.attributes.href
import org.jetbrains.compose.web.attributes.label
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.attributes.value
import kotlinx.browser.document
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
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
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLHRElement
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

typealias AttrBuilderContext<T> = AttrsBuilder<T>.() -> Unit
typealias ContentBuilder<T> = @Composable ElementScope<T>.() -> Unit

interface ElementBuilder<TElement : Element> {
    fun create(): TElement

    companion object {
        fun <TElement : Element> createBuilder(tagName: String): ElementBuilder<TElement> {
            return object  : ElementBuilderImplementation<TElement>(tagName) {}
        }
    }
}

private open class ElementBuilderImplementation<TElement : Element>(private val tagName: String) : ElementBuilder<TElement> {
    private val el: Element by lazy { document.createElement(tagName) }
    override fun create(): TElement = el.cloneNode() as TElement
}

private object DomBuilder {
    val Address: ElementBuilder<HTMLElement> = ElementBuilderImplementation("address")
    val Article: ElementBuilder<HTMLElement> = ElementBuilderImplementation("article")
    val Aside: ElementBuilder<HTMLElement> = ElementBuilderImplementation("aside")
    val Header: ElementBuilder<HTMLElement> = ElementBuilderImplementation("header")

    val Area: ElementBuilder<HTMLAreaElement> = ElementBuilderImplementation("area")
    val Audio: ElementBuilder<HTMLAudioElement> = ElementBuilderImplementation("audio")
    val Map: ElementBuilder<HTMLMapElement> = ElementBuilderImplementation("map")
    val Track: ElementBuilder<HTMLTrackElement> = ElementBuilderImplementation("track")
    val Video: ElementBuilder<HTMLVideoElement> = ElementBuilderImplementation("video")

    val Datalist: ElementBuilder<HTMLDataListElement> = ElementBuilderImplementation("datalist")
    val Fieldset: ElementBuilder<HTMLFieldSetElement> = ElementBuilderImplementation("fieldset")
    val Legend: ElementBuilder<HTMLLegendElement> = ElementBuilderImplementation("legend")
    val Meter: ElementBuilder<HTMLMeterElement> = ElementBuilderImplementation("meter")
    val Output: ElementBuilder<HTMLOutputElement> = ElementBuilderImplementation("output")
    val Progress: ElementBuilder<HTMLProgressElement> = ElementBuilderImplementation("progress")

    val Embed: ElementBuilder<HTMLEmbedElement> = ElementBuilderImplementation("embed")
    val Iframe: ElementBuilder<HTMLIFrameElement> = ElementBuilderImplementation("iframe")
    val Object: ElementBuilder<HTMLObjectElement> = ElementBuilderImplementation("object")
    val Param: ElementBuilder<HTMLParamElement> = ElementBuilderImplementation("param")
    val Picture: ElementBuilder<HTMLPictureElement> = ElementBuilderImplementation("picture")
    val Source: ElementBuilder<HTMLSourceElement> = ElementBuilderImplementation("source")

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
    val Label: ElementBuilder<HTMLLabelElement> = ElementBuilderImplementation("label")
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


@Composable
fun Address(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Address,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Article(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Article,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Aside(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Aside,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Header(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Header,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Area(
    attrs: AttrBuilderContext<HTMLAreaElement> = {},
    content: ContentBuilder<HTMLAreaElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Area,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Audio(
    attrs: AttrBuilderContext<HTMLAudioElement> = {},
    content: ContentBuilder<HTMLAudioElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Audio,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun HTMLMap(
    attrs: AttrBuilderContext<HTMLMapElement> = {},
    content: ContentBuilder<HTMLMapElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Map,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Track(
    attrs: AttrBuilderContext<HTMLTrackElement> = {},
    content: ContentBuilder<HTMLTrackElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Track,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Video(
    attrs: AttrBuilderContext<HTMLVideoElement> = {},
    content: ContentBuilder<HTMLVideoElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Video,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Datalist(
    attrs: AttrBuilderContext<HTMLDataListElement> = {},
    content: ContentBuilder<HTMLDataListElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Datalist,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Fieldset(
    attrs: AttrBuilderContext<HTMLFieldSetElement> = {},
    content: ContentBuilder<HTMLFieldSetElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Fieldset,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Legend(
    attrs: AttrBuilderContext<HTMLLegendElement> = {},
    content: ContentBuilder<HTMLLegendElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Legend,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Meter(
    attrs: AttrBuilderContext<HTMLMeterElement> = {},
    content: ContentBuilder<HTMLMeterElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Meter,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Output(
    attrs: AttrBuilderContext<HTMLOutputElement> = {},
    content: ContentBuilder<HTMLOutputElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Output,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Progress(
    attrs: AttrBuilderContext<HTMLProgressElement> = {},
    content: ContentBuilder<HTMLProgressElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Progress,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Embed(
    attrs: AttrBuilderContext<HTMLEmbedElement> = {},
    content: ContentBuilder<HTMLEmbedElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Embed,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Iframe(
    attrs: AttrBuilderContext<HTMLIFrameElement> = {},
    content: ContentBuilder<HTMLIFrameElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Iframe,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Object(
    attrs: AttrBuilderContext<HTMLObjectElement> = {},
    content: ContentBuilder<HTMLObjectElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Object,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Param(
    attrs: AttrBuilderContext<HTMLParamElement> = {},
    content: ContentBuilder<HTMLParamElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Param,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Picture(
    attrs: AttrBuilderContext<HTMLPictureElement> = {},
    content: ContentBuilder<HTMLPictureElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Picture,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Source(
    attrs: AttrBuilderContext<HTMLSourceElement> = {},
    content: ContentBuilder<HTMLSourceElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Source,
        applyAttrs = attrs,
        content = content
    )
}

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
    attrs: AttrBuilderContext<HTMLDivElement> = {},
    content: ContentBuilder<HTMLDivElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Div,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun A(
    href: String? = null,
    attrs: AttrBuilderContext<HTMLAnchorElement> = {},
    content: ContentBuilder<HTMLAnchorElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.A,
        applyAttrs = {
            if (href != null) {
                this.href(href)
            }
            attrs()
        },
        content = content
    )
}

@Composable
fun Input(
    type: InputType = InputType.Text,
    value: String = "",
    attrs: AttrBuilderContext<HTMLInputElement> = {}
) {
    TagElement(
        elementBuilder = DomBuilder.Input,
        applyAttrs = {
            type(type)
            value(value)
            attrs()
        },
        content = null
    )
}

@Composable
fun Button(
    attrs: AttrBuilderContext<HTMLButtonElement> = {},
    content: ContentBuilder<HTMLButtonElement>? = null
) = TagElement(elementBuilder = DomBuilder.Button, applyAttrs = attrs, content = content)

@Composable
fun H1(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = DomBuilder.H1, applyAttrs = attrs, content = content)

@Composable
fun H2(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = DomBuilder.H2, applyAttrs = attrs, content = content)

@Composable
fun H3(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = DomBuilder.H3, applyAttrs = attrs, content = content)

@Composable
fun H4(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = DomBuilder.H4, applyAttrs = attrs, content = content)

@Composable
fun H5(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = DomBuilder.H5, applyAttrs = attrs, content = content)

@Composable
fun H6(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = DomBuilder.H6, applyAttrs = attrs, content = content)

@Composable
fun P(
    attrs: AttrBuilderContext<HTMLParagraphElement> = {},
    content: ContentBuilder<HTMLParagraphElement>? = null
) = TagElement(elementBuilder = DomBuilder.P, applyAttrs = attrs, content = content)

@Composable
fun Em(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = DomBuilder.Em, applyAttrs = attrs, content = content)

@Composable
fun I(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = DomBuilder.I, applyAttrs = attrs, content = content)

@Composable
fun B(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = DomBuilder.B, applyAttrs = attrs, content = content)

@Composable
fun Small(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = DomBuilder.Small, applyAttrs = attrs, content = content)

@Composable
fun Span(
    attrs: AttrBuilderContext<HTMLSpanElement> = {},
    content: ContentBuilder<HTMLSpanElement>? = null
) = TagElement(elementBuilder = DomBuilder.Span, applyAttrs = attrs, content = content)

@Composable
fun Br(attrs: AttrBuilderContext<HTMLBRElement> = {}) =
    TagElement(elementBuilder = DomBuilder.Br, applyAttrs = attrs, content = null)

@Composable
fun Ul(
    attrs: AttrBuilderContext<HTMLUListElement> = {},
    content: ContentBuilder<HTMLUListElement>? = null
) = TagElement(elementBuilder = DomBuilder.Ul, applyAttrs = attrs, content = content)

@Composable
fun Ol(
    attrs: AttrBuilderContext<HTMLOListElement> = {},
    content: ContentBuilder<HTMLOListElement>? = null
) = TagElement(elementBuilder = DomBuilder.Ol, applyAttrs = attrs, content = content)

@Composable
fun DOMScope<HTMLOListElement>.Li(
    attrs: AttrBuilderContext<HTMLLIElement> = {},
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = DomBuilder.Li, applyAttrs = attrs, content = content)

@Composable
fun DOMScope<HTMLUListElement>.Li(
    attrs: AttrBuilderContext<HTMLLIElement> = {},
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = DomBuilder.Li, applyAttrs = attrs, content = content)

@Composable
fun Img(
    src: String,
    alt: String = "",
    attrs: AttrBuilderContext<HTMLImageElement> = {}
) = TagElement(
    elementBuilder = DomBuilder.Img,
    applyAttrs = {
        src(src).alt(alt)
        attrs()
    },
    content = null
)

@Composable
fun Form(
    action: String? = null,
    attrs: AttrBuilderContext<HTMLFormElement> = {},
    content: ContentBuilder<HTMLFormElement>? = null
) = TagElement(
    elementBuilder = DomBuilder.Form,
    applyAttrs = {
        if (!action.isNullOrEmpty()) action(action)
        attrs()
    },
    content = content
)

@Composable
fun Select(
    attrs: AttrBuilderContext<HTMLSelectElement> = {},
    content: ContentBuilder<HTMLSelectElement>? = null
) = TagElement(
    elementBuilder = DomBuilder.Select,
    applyAttrs = attrs,
    content = content
)

@Composable
fun Option(
    value: String,
    attrs: AttrBuilderContext<HTMLOptionElement> = {},
    content: ContentBuilder<HTMLOptionElement>? = null
) = TagElement(
    elementBuilder = DomBuilder.Option,
    applyAttrs = {
        value(value)
        attrs()
    },
    content = content
)

@Composable
fun OptGroup(
    label: String,
    attrs: AttrBuilderContext<HTMLOptGroupElement> = {},
    content: ContentBuilder<HTMLOptGroupElement>? = null
) = TagElement(
    elementBuilder = DomBuilder.OptGroup,
    applyAttrs = {
        label(label)
        attrs()
    },
    content = content
)

@Composable
fun Section(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(
    elementBuilder = DomBuilder.Section,
    applyAttrs = attrs,
    content = content
)

@Composable
fun TextArea(
    attrs: AttrBuilderContext<HTMLTextAreaElement> = {},
    value: String
) = TagElement(
    elementBuilder = DomBuilder.TextArea,
    applyAttrs = {
        value(value)
        attrs()
    }
) {
    Text(value)
}

@Composable
fun Nav(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(
    elementBuilder = DomBuilder.Nav,
    applyAttrs = attrs,
    content = content
)

@Composable
fun Pre(
    attrs: AttrBuilderContext<HTMLPreElement> = {},
    content: ContentBuilder<HTMLPreElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Pre,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Code(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Code,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Main(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Main,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Footer(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Footer,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Hr(
    attrs: AttrBuilderContext<HTMLHRElement> = {}
) {
    TagElement(
        elementBuilder = DomBuilder.Hr,
        applyAttrs = attrs,
        content = null
    )
}

@Composable
fun Label(
    forId: String? = null,
    attrs: AttrBuilderContext<HTMLLabelElement> = {},
    content: ContentBuilder<HTMLLabelElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Label,
        applyAttrs = {
            if (forId != null) {
                forId(forId)
            }
            attrs()
        },
        content = content
    )
}

@Composable
fun Table(
    attrs: AttrBuilderContext<HTMLTableElement> = {},
    content: ContentBuilder<HTMLTableElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Table,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Caption(
    attrs: AttrBuilderContext<HTMLTableCaptionElement> = {},
    content: ContentBuilder<HTMLTableCaptionElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Caption,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Col(
    attrs: AttrBuilderContext<HTMLTableColElement> = {}
) {
    TagElement(
        elementBuilder = DomBuilder.Col,
        applyAttrs = attrs,
        content = null
    )
}

@Composable
fun Colgroup(
    attrs: AttrBuilderContext<HTMLTableColElement> = {},
    content: ContentBuilder<HTMLTableColElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Colgroup,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tr(
    attrs: AttrBuilderContext<HTMLTableRowElement> = {},
    content: ContentBuilder<HTMLTableRowElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Tr,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Thead(
    attrs: AttrBuilderContext<HTMLTableSectionElement> = {},
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Thead,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Th(
    attrs: AttrBuilderContext<HTMLTableCellElement> = {},
    content: ContentBuilder<HTMLTableCellElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Th,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Td(
    attrs: AttrBuilderContext<HTMLTableCellElement> = {},
    content: ContentBuilder<HTMLTableCellElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Td,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tbody(
    attrs: AttrBuilderContext<HTMLTableSectionElement> = {},
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Tbody,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tfoot(
    attrs: AttrBuilderContext<HTMLTableSectionElement> = {},
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = DomBuilder.Tfoot,
        applyAttrs = attrs,
        content = content
    )
}

/**
 * Use this function to mount the <style> tag into the DOM tree.
 *
 * @param cssRules - is a list of style rules.
 * Usually, it's [org.jetbrains.compose.web.css.StyleSheet] instance
 */
@Composable
fun Style(
    applyAttrs: AttrsBuilder<HTMLStyleElement>.() -> Unit = {},
    cssRules: CSSRuleDeclarationList
) {
    TagElement(
        elementBuilder = DomBuilder.Style,
        applyAttrs = {
            applyAttrs()
        },
    ) {
        DomSideEffect(cssRules) { style ->
            style.sheet?.let { sheet ->
                setCSSRules(sheet, cssRules)
                onDispose {
                    clearCSSRules(sheet)
                }
            }
        }
    }
}
