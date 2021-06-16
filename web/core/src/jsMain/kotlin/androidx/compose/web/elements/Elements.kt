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

@Composable
fun Address(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = { createElement("address") },
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
        elementBuilder = { createElement("article") },
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
        elementBuilder = { createElement("aside") },
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
        elementBuilder = { createElement("header") },
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
        elementBuilder = { createElement("area") },
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
        elementBuilder = { createElement("audio") },
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
        elementBuilder = { createElement("map") },
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
        elementBuilder = { createElement("track") },
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
        elementBuilder = { createElement("video") },
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
        elementBuilder = { createElement("datalist") },
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
        elementBuilder = { createElement("fieldset") },
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
        elementBuilder = { createElement("legend") },
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
        elementBuilder = { createElement("meter") },
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
        elementBuilder = { createElement("output") },
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
        elementBuilder = { createElement("progress") },
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
        elementBuilder = { createElement("embed") },
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
        elementBuilder = { createElement("iframe") },
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
        elementBuilder = { createElement("object") },
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
        elementBuilder = { createElement("param") },
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
        elementBuilder = { createElement("picture") },
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
        elementBuilder = { createElement("source") },
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
        elementBuilder = { createElement("div") },
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
        elementBuilder = { createElement("a") },
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
        elementBuilder = { createElement<HTMLInputElement>("input") },
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
) = TagElement(elementBuilder = { createElement("button") }, applyAttrs = attrs, content = content)

@Composable
fun H1(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { createElement("h1") }, applyAttrs = attrs, content = content)

@Composable
fun H2(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { createElement("h2") }, applyAttrs = attrs, content = content)

@Composable
fun H3(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { createElement("h3") }, applyAttrs = attrs, content = content)

@Composable
fun H4(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { createElement("h4") }, applyAttrs = attrs, content = content)

@Composable
fun H5(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { createElement("h5") }, applyAttrs = attrs, content = content)

@Composable
fun H6(
    attrs: AttrBuilderContext<HTMLHeadingElement> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { createElement("h6") }, applyAttrs = attrs, content = content)

@Composable
fun P(
    attrs: AttrBuilderContext<HTMLParagraphElement> = {},
    content: ContentBuilder<HTMLParagraphElement>? = null
) = TagElement(elementBuilder = { createElement("p") }, applyAttrs = attrs, content = content)

@Composable
fun Em(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = { createElement("em") }, applyAttrs = attrs, content = content)

@Composable
fun I(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = { createElement("i") }, applyAttrs = attrs, content = content)

@Composable
fun B(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = { createElement("b") }, applyAttrs = attrs, content = content)

@Composable
fun Small(
    attrs: AttrBuilderContext<HTMLElement> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = { createElement("small") }, applyAttrs = attrs, content = content)

@Composable
fun Span(
    attrs: AttrBuilderContext<HTMLSpanElement> = {},
    content: ContentBuilder<HTMLSpanElement>? = null
) = TagElement(elementBuilder = { createElement("span") }, applyAttrs = attrs, content = content)

@Composable
fun Br(attrs: AttrBuilderContext<HTMLBRElement> = {}) =
    TagElement(elementBuilder = { createElement("br") }, applyAttrs = attrs, content = null)

@Composable
fun Ul(
    attrs: AttrBuilderContext<HTMLUListElement> = {},
    content: ContentBuilder<HTMLUListElement>? = null
) = TagElement(elementBuilder = { createElement("ul") }, applyAttrs = attrs, content = content)

@Composable
fun Ol(
    attrs: AttrBuilderContext<HTMLOListElement> = {},
    content: ContentBuilder<HTMLOListElement>? = null
) = TagElement(elementBuilder = { createElement("ol") }, applyAttrs = attrs, content = content)

@Composable
fun DOMScope<HTMLOListElement>.Li(
    attrs: AttrBuilderContext<HTMLLIElement> = {},
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = { createElement("li") }, applyAttrs = attrs, content = content)

@Composable
fun DOMScope<HTMLUListElement>.Li(
    attrs: AttrBuilderContext<HTMLLIElement> = {},
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = { createElement("li") }, applyAttrs = attrs, content = content)

@Composable
fun Img(
    src: String,
    alt: String = "",
    attrs: AttrBuilderContext<HTMLImageElement> = {}
) = TagElement(
    elementBuilder = { createElement<HTMLImageElement>("img") },
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
    elementBuilder = { createElement("form") },
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
    elementBuilder = { createElement("select") },
    applyAttrs = attrs,
    content = content
)

@Composable
fun Option(
    value: String,
    attrs: AttrBuilderContext<HTMLOptionElement> = {},
    content: ContentBuilder<HTMLOptionElement>? = null
) = TagElement(
    elementBuilder = { createElement("option") },
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
    elementBuilder = { createElement("optgroup") },
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
    elementBuilder = { createElement("section") },
    applyAttrs = attrs,
    content = content
)

@Composable
fun TextArea(
    attrs: AttrBuilderContext<HTMLTextAreaElement> = {},
    value: String
) = TagElement(
    elementBuilder = { createElement<HTMLTextAreaElement>("textarea") },
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
    elementBuilder = { createElement("nav") },
    applyAttrs = attrs,
    content = content
)

@Composable
fun Pre(
    attrs: AttrBuilderContext<HTMLPreElement> = {},
    content: ContentBuilder<HTMLPreElement>? = null
) {
    TagElement(
        elementBuilder = { createElement("pre") },
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
        elementBuilder = { createElement("code") },
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
        elementBuilder = { createElement("main") },
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
        elementBuilder = { createElement("footer") },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Hr(
    attrs: AttrBuilderContext<HTMLHRElement> = {}
) {
    TagElement(
        elementBuilder = { createElement("hr") },
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
        elementBuilder = { createElement("label") },
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
        elementBuilder = { createElement("table") },
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
        elementBuilder = { createElement("caption") },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Col(
    attrs: AttrBuilderContext<HTMLTableColElement> = {}
) {
    TagElement(
        elementBuilder = { createElement("col") },
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
        elementBuilder = { createElement("colgroup") },
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
        elementBuilder = { createElement("tr") },
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
        elementBuilder = { createElement("thead") },
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
        elementBuilder = { createElement("th") },
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
        elementBuilder = { createElement("td") },
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
        elementBuilder = { createElement("tbody") },
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
        elementBuilder = { createElement("tfoot") },
        applyAttrs = attrs,
        content = content
    )
}
