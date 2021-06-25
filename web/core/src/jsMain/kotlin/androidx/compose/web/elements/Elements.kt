package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.web.attributes.InputAttrsBuilder
import androidx.compose.web.attributes.TextAreaAttrsBuilder
import org.jetbrains.compose.web.DomApplier
import org.jetbrains.compose.web.DomNodeWrapper
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.*
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
import org.w3c.dom.HTMLTrackElement
import org.w3c.dom.HTMLUListElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.Text

typealias AttrBuilderContext<T> = AttrsBuilder<T>.() -> Unit
typealias ContentBuilder<T> = @Composable ElementScope<T>.() -> Unit

@Composable
fun Address(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = ElementBuilder.Address,
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
        elementBuilder = ElementBuilder.Article,
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
        elementBuilder = ElementBuilder.Aside,
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
        elementBuilder = ElementBuilder.Header,
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
        elementBuilder = ElementBuilder.Area,
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
        elementBuilder = ElementBuilder.Audio,
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
        elementBuilder = ElementBuilder.Map,
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
        elementBuilder = ElementBuilder.Track,
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
        elementBuilder = ElementBuilder.Video,
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
        elementBuilder = ElementBuilder.Datalist,
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
        elementBuilder = ElementBuilder.Fieldset,
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
        elementBuilder = ElementBuilder.Legend,
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
        elementBuilder = ElementBuilder.Meter,
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
        elementBuilder = ElementBuilder.Output,
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
        elementBuilder = ElementBuilder.Progress,
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
        elementBuilder = ElementBuilder.Embed,
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
        elementBuilder = ElementBuilder.Iframe,
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
        elementBuilder = ElementBuilder.Object,
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
        elementBuilder = ElementBuilder.Param,
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
        elementBuilder = ElementBuilder.Picture,
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
        elementBuilder = ElementBuilder.Source,
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
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    TagElement(
        elementBuilder = ElementBuilder.Div,
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
        elementBuilder = ElementBuilder.A,
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
) = TagElement(elementBuilder = ElementBuilder.Button, applyAttrs = attrs, content = content)

@Composable
fun H1(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = ElementBuilder.H1, applyAttrs = attrs, content = content)

@Composable
fun H2(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = ElementBuilder.H2, applyAttrs = attrs, content = content)

@Composable
fun H3(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = ElementBuilder.H3, applyAttrs = attrs, content = content)

@Composable
fun H4(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = ElementBuilder.H4, applyAttrs = attrs, content = content)

@Composable
fun H5(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = ElementBuilder.H5, applyAttrs = attrs, content = content)

@Composable
fun H6(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = ElementBuilder.H6, applyAttrs = attrs, content = content)

@Composable
fun P(
    attrs: AttrBuilderContext<HTMLParagraphElement>? = null,
    content: ContentBuilder<HTMLParagraphElement>? = null
) = TagElement(elementBuilder = ElementBuilder.P, applyAttrs = attrs, content = content)

@Composable
fun Em(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = ElementBuilder.Em, applyAttrs = attrs, content = content)

@Composable
fun I(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = ElementBuilder.I, applyAttrs = attrs, content = content)

@Composable
fun B(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = ElementBuilder.B, applyAttrs = attrs, content = content)

@Composable
fun Small(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = ElementBuilder.Small, applyAttrs = attrs, content = content)

@Composable
fun Span(
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
    content: ContentBuilder<HTMLSpanElement>? = null
) = TagElement(elementBuilder = ElementBuilder.Span, applyAttrs = attrs, content = content)

@Composable
fun Br(attrs: AttrBuilderContext<HTMLBRElement>? = null) =
    TagElement(elementBuilder = ElementBuilder.Br, applyAttrs = attrs, content = null)

@Composable
fun Ul(
    attrs: AttrBuilderContext<HTMLUListElement>? = null,
    content: ContentBuilder<HTMLUListElement>? = null
) = TagElement(elementBuilder = ElementBuilder.Ul, applyAttrs = attrs, content = content)

@Composable
fun Ol(
    attrs: AttrBuilderContext<HTMLOListElement>? = null,
    content: ContentBuilder<HTMLOListElement>? = null
) = TagElement(elementBuilder = ElementBuilder.Ol, applyAttrs = attrs, content = content)

@Composable
fun DOMScope<HTMLOListElement>.Li(
    attrs: AttrBuilderContext<HTMLLIElement>? = null,
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = ElementBuilder.Li, applyAttrs = attrs, content = content)

@Composable
fun DOMScope<HTMLUListElement>.Li(
    attrs: AttrBuilderContext<HTMLLIElement>? = null,
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = ElementBuilder.Li, applyAttrs = attrs, content = content)

@Composable
fun Img(
    src: String,
    alt: String = "",
    attrs: AttrBuilderContext<HTMLImageElement>? = null
) = TagElement(
    elementBuilder = ElementBuilder.Img,
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
    elementBuilder = ElementBuilder.Form,
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
    attrs: AttrBuilderContext<HTMLSelectElement>? = null,
    content: ContentBuilder<HTMLSelectElement>? = null
) = TagElement(
    elementBuilder = ElementBuilder.Select,
    applyAttrs = attrs,
    content = content
)

@Composable
fun Option(
    value: String,
    attrs: AttrBuilderContext<HTMLOptionElement>? = null,
    content: ContentBuilder<HTMLOptionElement>? = null
) = TagElement(
    elementBuilder = ElementBuilder.Option,
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
    elementBuilder = ElementBuilder.OptGroup,
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
    elementBuilder = ElementBuilder.Section,
    applyAttrs = attrs,
    content = content
)

@Composable
fun TextArea(
    attrs: (TextAreaAttrsBuilder.() -> Unit)? = null,
    value: String
) = TagElement(
    elementBuilder = ElementBuilder.TextArea,
    applyAttrs = {
        val  taab = TextAreaAttrsBuilder()
        if (attrs != null) {
            taab.attrs()
        }
        taab.value(value)
        this.copyFrom(taab)
    }
) {
    Text(value)
}

@Composable
fun Nav(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(
    elementBuilder = ElementBuilder.Nav,
    applyAttrs = attrs,
    content = content
)

@Composable
fun Pre(
    attrs: AttrBuilderContext<HTMLPreElement>? = null,
    content: ContentBuilder<HTMLPreElement>? = null
) {
    TagElement(
        elementBuilder = ElementBuilder.Pre,
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
        elementBuilder = ElementBuilder.Code,
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
        elementBuilder = ElementBuilder.Main,
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
        elementBuilder = ElementBuilder.Footer,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Hr(
    attrs: AttrBuilderContext<HTMLHRElement>? = null
) {
    TagElement(
        elementBuilder = ElementBuilder.Hr,
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
        elementBuilder = ElementBuilder.Label,
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
        elementBuilder = ElementBuilder.Table,
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
        elementBuilder = ElementBuilder.Caption,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Col(
    attrs: AttrBuilderContext<HTMLTableColElement>? = null
) {
    TagElement(
        elementBuilder = ElementBuilder.Col,
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
        elementBuilder = ElementBuilder.Colgroup,
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
        elementBuilder = ElementBuilder.Tr,
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
        elementBuilder = ElementBuilder.Thead,
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
        elementBuilder = ElementBuilder.Th,
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
        elementBuilder = ElementBuilder.Td,
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
        elementBuilder = ElementBuilder.Tbody,
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
        elementBuilder = ElementBuilder.Tfoot,
        applyAttrs = attrs,
        content = content
    )
}
