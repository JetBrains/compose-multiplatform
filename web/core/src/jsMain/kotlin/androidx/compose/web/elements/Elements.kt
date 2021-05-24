package androidx.compose.web.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.web.DomApplier
import androidx.compose.web.DomNodeWrapper
import androidx.compose.web.attributes.AttrsBuilder
import androidx.compose.web.attributes.InputType
import androidx.compose.web.attributes.Tag
import androidx.compose.web.attributes.action
import androidx.compose.web.attributes.alt
import androidx.compose.web.attributes.forId
import androidx.compose.web.attributes.href
import androidx.compose.web.attributes.label
import androidx.compose.web.attributes.src
import androidx.compose.web.attributes.type
import androidx.compose.web.attributes.value
import androidx.compose.web.css.StyleBuilder
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLBRElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
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
import org.w3c.dom.Text

typealias StyleBuilderContext = StyleBuilder.() -> Unit
typealias AttrBuilderContext<T> = AttrsBuilder<T>.() -> Unit
typealias ContentBuilder<T> = @Composable ElementScope<T>.() -> Unit

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
inline fun Div(
    crossinline attrs: AttrBuilderContext<Tag.Div> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLDivElement>
) {
    TagElement(
        tagName = "div",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun A(
    href: String? = null,
    crossinline attrs: AttrBuilderContext<Tag.A> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLAnchorElement>
) {
    TagElement<Tag.A, HTMLAnchorElement>(
        tagName = "a",
        applyAttrs = {
            href(href)
            attrs()
        },
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Input(
    type: InputType = InputType.Text,
    value: String = "",
    crossinline attrs: AttrBuilderContext<Tag.Input> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLInputElement> = {}
) {
    TagElement<Tag.Input, HTMLInputElement>(
        tagName = "input",
        applyAttrs = {
            type(type)
            value(value)
            attrs()
        },
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Button(
    crossinline attrs: AttrBuilderContext<Tag.Button> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLHeadingElement>
) = TagElement("button", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun H1(
    crossinline attrs: AttrBuilderContext<Tag.H> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLHeadingElement>
) = TagElement("h1", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun H2(
    crossinline attrs: AttrBuilderContext<Tag.H> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLHeadingElement>
) = TagElement("h2", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun H3(
    crossinline attrs: AttrBuilderContext<Tag.H> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLHeadingElement>
) = TagElement("h3", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun H4(
    crossinline attrs: AttrBuilderContext<Tag.H> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLHeadingElement>
) = TagElement("h4", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun H5(
    crossinline attrs: AttrBuilderContext<Tag.H> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLHeadingElement>
) = TagElement("h5", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun H6(
    crossinline attrs: AttrBuilderContext<Tag.H> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLHeadingElement>
) = TagElement("h6", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun P(
    crossinline attrs: AttrBuilderContext<Tag.P> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLParagraphElement>
) = TagElement("p", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun Em(
    crossinline attrs: AttrBuilderContext<Tag> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement>
) = TagElement("em", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun I(
    crossinline attrs: AttrBuilderContext<Tag> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement>
) = TagElement("i", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun B(
    crossinline attrs: AttrBuilderContext<Tag> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement>
) = TagElement("b", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun Small(
    crossinline attrs: AttrBuilderContext<Tag> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement>
) = TagElement("small", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun Span(
    crossinline attrs: AttrBuilderContext<Tag.Span> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLSpanElement>
) = TagElement("span", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun Br(
    crossinline attrs: AttrBuilderContext<Tag.Br> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLBRElement>
) = TagElement("br", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun Ul(
    crossinline attrs: AttrBuilderContext<Tag.Ul> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLUListElement>,
) = TagElement("ul", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun Ol(
    crossinline attrs: AttrBuilderContext<Tag.Ol> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLOListElement>
) = TagElement("ol", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun DOMScope<HTMLOListElement>.Li(
    crossinline attrs: AttrBuilderContext<Tag.Li> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLLIElement>
) = TagElement("li", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun DOMScope<HTMLUListElement>.Li(
    crossinline attrs: AttrBuilderContext<Tag.Li> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLLIElement>
) = TagElement("li", applyAttrs = attrs, applyStyle = style, content = content)

@Composable
inline fun Img(
    src: String,
    alt: String = "",
    crossinline attrs: AttrBuilderContext<Tag.Img> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLImageElement> = {}
) = TagElement<Tag.Img, HTMLImageElement>(
    tagName = "img",
    applyAttrs = {
        src(src).alt(alt)
        attrs()
    },
    applyStyle = style, content = content
)

@Composable
inline fun Form(
    action: String? = null,
    crossinline attrs: AttrBuilderContext<Tag.Form> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLFormElement>
) = TagElement<Tag.Form, HTMLFormElement>(
    tagName = "form",
    applyAttrs = {
        if (!action.isNullOrEmpty()) action(action)
        attrs()
    },
    applyStyle = style, content = content
)

@Composable
inline fun Select(
    crossinline attrs: AttrBuilderContext<Tag.Select> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLSelectElement>
) = TagElement(
    tagName = "select",
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun DOMScope<HTMLUListElement>.Option(
    value: String,
    crossinline attrs: AttrBuilderContext<Tag.Option> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLOptionElement>
) = TagElement<Tag.Option, HTMLOptionElement>(
    tagName = "option",
    applyAttrs = {
        value(value)
        attrs()
    },
    applyStyle = style,
    content = content
)

@Composable
inline fun OptGroup(
    label: String,
    crossinline attrs: AttrBuilderContext<Tag.OptGroup> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLOptGroupElement>
) = TagElement<Tag.OptGroup, HTMLOptGroupElement>(
    tagName = "optgroup",
    applyAttrs = {
        label(label)
        attrs()
    },
    applyStyle = style,
    content = content
)

@Composable
inline fun Section(
    crossinline attrs: AttrBuilderContext<Tag> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement>
) = TagElement(
    tagName = "section",
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun TextArea(
    crossinline attrs: AttrBuilderContext<Tag.TextArea> = {},
    crossinline style: StyleBuilderContext = {},
    value: String
) = TagElement<Tag.TextArea, HTMLTextAreaElement>(
    tagName = "textarea",
    applyAttrs = {
        value(value)
        attrs()
    },
    applyStyle = style
) {
    Text(value)
}

@Composable
inline fun Nav(
    crossinline attrs: AttrBuilderContext<Tag.Nav> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement>
) = TagElement(
    tagName = "nav",
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Pre(
    crossinline attrs: AttrBuilderContext<Tag.Pre> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLPreElement>
) {
    TagElement(
        tagName = "pre",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Code(
    crossinline attrs: AttrBuilderContext<Tag.Code> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement>
) {
    TagElement(
        tagName = "code",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Main(
    crossinline attrs: AttrBuilderContext<Tag.Div> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement> = {}
) {
    TagElement<Tag.Div, HTMLAnchorElement>(
        tagName = "main",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Footer(
    crossinline attrs: AttrBuilderContext<Tag.Div> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement> = {}
) {
    TagElement<Tag.Div, HTMLAnchorElement>(
        tagName = "footer",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Label(
    forId: String? = null,
    crossinline attrs: AttrBuilderContext<Tag.Label> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLElement> = {}
) {
    TagElement<Tag.Label, HTMLAnchorElement>(
        tagName = "label",
        applyAttrs = {
            forId(forId)
            attrs()
        },
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Table(
    crossinline attrs: AttrBuilderContext<Tag.Table> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableElement>
) {
    TagElement(
        tagName = "table",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Caption(
    crossinline attrs: AttrBuilderContext<Tag.Caption> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableCaptionElement>
) {
    TagElement(
        tagName = "caption",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Col(
    crossinline attrs: AttrBuilderContext<Tag.Col> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableColElement>
) {
    TagElement(
        tagName = "col",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Colgroup(
    crossinline attrs: AttrBuilderContext<Tag.Colgroup> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableColElement>
) {
    TagElement(
        tagName = "colgroup",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Tr(
    crossinline attrs: AttrBuilderContext<Tag.Tr> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableRowElement>
) {
    TagElement(
        tagName = "tr",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Thead(
    crossinline attrs: AttrBuilderContext<Tag.Thead> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableSectionElement>
) {
    TagElement(
        tagName = "thead",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Th(
    crossinline attrs: AttrBuilderContext<Tag.Th> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableCellElement>
) {
    TagElement(
        tagName = "th",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Td(
    crossinline attrs: AttrBuilderContext<Tag.Td> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableCellElement>
) {
    TagElement(
        tagName = "td",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Tbody(
    crossinline attrs: AttrBuilderContext<Tag.Tbody> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableSectionElement>
) {
    TagElement(
        tagName = "tbody",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Tfoot(
    crossinline attrs: AttrBuilderContext<Tag.Tfoot> = {},
    crossinline style: StyleBuilderContext = {},
    content: ContentBuilder<HTMLTableSectionElement>
) {
    TagElement(
        tagName = "tfoot",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}
