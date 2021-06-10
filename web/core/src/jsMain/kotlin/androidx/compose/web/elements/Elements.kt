package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.jetbrains.compose.web.DomApplier
import org.jetbrains.compose.web.DomNodeWrapper
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.Tag
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
import org.w3c.dom.HTMLBRElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLHRElement
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
fun Div(
    attrs: AttrBuilderContext<Tag.Div> = {},
    content: ContentBuilder<HTMLDivElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Div.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun A(
    href: String? = null,
    attrs: AttrBuilderContext<Tag.A> = {},
    content: ContentBuilder<HTMLAnchorElement>? = null
) {
    TagElement<Tag.A, HTMLAnchorElement>(
        elementBuilder = { ElementBuilder.A.create() },
        applyAttrs = {
            href(href)
            attrs()
        },
        content = content
    )
}

@Composable
fun Input(
    type: InputType = InputType.Text,
    value: String = "",
    attrs: AttrBuilderContext<Tag.Input> = {}
) {
    TagElement<Tag.Input, HTMLInputElement>(
        elementBuilder = { ElementBuilder.Input.create() },
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
    attrs: AttrBuilderContext<Tag.Button> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.Button.create() }, applyAttrs = attrs, content = content)

@Composable
fun H1(
    attrs: AttrBuilderContext<Tag.H> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.H1.create() }, applyAttrs = attrs, content = content)

@Composable
fun H2(
    attrs: AttrBuilderContext<Tag.H> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.H2.create() }, applyAttrs = attrs, content = content)

@Composable
fun H3(
    attrs: AttrBuilderContext<Tag.H> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.H3.create() }, applyAttrs = attrs, content = content)

@Composable
fun H4(
    attrs: AttrBuilderContext<Tag.H> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.H4.create() }, applyAttrs = attrs, content = content)

@Composable
fun H5(
    attrs: AttrBuilderContext<Tag.H> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.H5.create() }, applyAttrs = attrs, content = content)

@Composable
fun H6(
    attrs: AttrBuilderContext<Tag.H> = {},
    content: ContentBuilder<HTMLHeadingElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.H6.create() }, applyAttrs = attrs, content = content)

@Composable
fun P(
    attrs: AttrBuilderContext<Tag.P> = {},
    content: ContentBuilder<HTMLParagraphElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.P.create() }, applyAttrs = attrs, content = content)

@Composable
fun Em(
    attrs: AttrBuilderContext<Tag> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.Em.create() }, applyAttrs = attrs, content = content)

@Composable
fun I(
    attrs: AttrBuilderContext<Tag> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.I.create() }, applyAttrs = attrs, content = content)

@Composable
fun B(
    attrs: AttrBuilderContext<Tag> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.B.create() }, applyAttrs = attrs, content = content)

@Composable
fun Small(
    attrs: AttrBuilderContext<Tag> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.Small.create() }, applyAttrs = attrs, content = content)

@Composable
fun Span(
    attrs: AttrBuilderContext<Tag.Span> = {},
    content: ContentBuilder<HTMLSpanElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.Span.create() }, applyAttrs = attrs, content = content)

@Composable
fun Br(attrs: AttrBuilderContext<Tag.Br> = {}) =
    TagElement<Tag.Br, HTMLBRElement>(elementBuilder = { ElementBuilder.Br.create() }, applyAttrs = attrs, content = null)

@Composable
fun Ul(
    attrs: AttrBuilderContext<Tag.Ul> = {},
    content: ContentBuilder<HTMLUListElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.Ul.create() }, applyAttrs = attrs, content = content)

@Composable
fun Ol(
    attrs: AttrBuilderContext<Tag.Ol> = {},
    content: ContentBuilder<HTMLOListElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.Ol.create() }, applyAttrs = attrs, content = content)

@Composable
fun DOMScope<HTMLOListElement>.Li(
    attrs: AttrBuilderContext<Tag.Li> = {},
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.Li.create() }, applyAttrs = attrs, content = content)

@Composable
fun DOMScope<HTMLUListElement>.Li(
    attrs: AttrBuilderContext<Tag.Li> = {},
    content: ContentBuilder<HTMLLIElement>? = null
) = TagElement(elementBuilder = { ElementBuilder.Li.create() }, applyAttrs = attrs, content = content)

@Composable
fun Img(
    src: String,
    alt: String = "",
    attrs: AttrBuilderContext<Tag.Img> = {}
) = TagElement<Tag.Img, HTMLImageElement>(
    elementBuilder = { ElementBuilder.Img.create() },
    applyAttrs = {
        src(src).alt(alt)
        attrs()
    },
    content = null
)

@Composable
fun Form(
    action: String? = null,
    attrs: AttrBuilderContext<Tag.Form> = {},
    content: ContentBuilder<HTMLFormElement>? = null
) = TagElement<Tag.Form, HTMLFormElement>(
    elementBuilder = { ElementBuilder.Form.create() },
    applyAttrs = {
        if (!action.isNullOrEmpty()) action(action)
        attrs()
    },
    content = content
)

@Composable
fun Select(
    attrs: AttrBuilderContext<Tag.Select> = {},
    content: ContentBuilder<HTMLSelectElement>? = null
) = TagElement(
    elementBuilder = { ElementBuilder.Select.create() },
    applyAttrs = attrs,
    content = content
)

@Composable
fun Option(
    value: String,
    attrs: AttrBuilderContext<Tag.Option> = {},
    content: ContentBuilder<HTMLOptionElement>? = null
) = TagElement<Tag.Option, HTMLOptionElement>(
    elementBuilder = { ElementBuilder.Option.create() },
    applyAttrs = {
        value(value)
        attrs()
    },
    content = content
)

@Composable
fun OptGroup(
    label: String,
    attrs: AttrBuilderContext<Tag.OptGroup> = {},
    content: ContentBuilder<HTMLOptGroupElement>? = null
) = TagElement<Tag.OptGroup, HTMLOptGroupElement>(
    elementBuilder = { ElementBuilder.OptGroup.create() },
    applyAttrs = {
        label(label)
        attrs()
    },
    content = content
)

@Composable
fun Section(
    attrs: AttrBuilderContext<Tag> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(
    elementBuilder = { ElementBuilder.Section.create() },
    applyAttrs = attrs,
    content = content
)

@Composable
fun TextArea(
    attrs: AttrBuilderContext<Tag.TextArea> = {},
    value: String
) = TagElement<Tag.TextArea, HTMLTextAreaElement>(
    elementBuilder = { ElementBuilder.TextArea.create() },
    applyAttrs = {
        value(value)
        attrs()
    }
) {
    Text(value)
}

@Composable
fun Nav(
    attrs: AttrBuilderContext<Tag.Nav> = {},
    content: ContentBuilder<HTMLElement>? = null
) = TagElement(
    elementBuilder = { ElementBuilder.Nav.create() },
    applyAttrs = attrs,
    content = content
)

@Composable
fun Pre(
    attrs: AttrBuilderContext<Tag.Pre> = {},
    content: ContentBuilder<HTMLPreElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Pre.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Code(
    attrs: AttrBuilderContext<Tag.Code> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Code.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Main(
    attrs: AttrBuilderContext<Tag.Div> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement<Tag.Div, HTMLAnchorElement>(
        elementBuilder = { ElementBuilder.Main.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Footer(
    attrs: AttrBuilderContext<Tag.Div> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement<Tag.Div, HTMLAnchorElement>(
        elementBuilder = { ElementBuilder.Footer.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Hr(
    attrs: AttrBuilderContext<Tag.Hr> = {}
) {
    TagElement<Tag.Hr, HTMLHRElement>(
        elementBuilder = { ElementBuilder.Hr.create() },
        applyAttrs = attrs,
        content = null
    )
}

@Composable
fun Label(
    forId: String? = null,
    attrs: AttrBuilderContext<Tag.Label> = {},
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement<Tag.Label, HTMLAnchorElement>(
        elementBuilder = { ElementBuilder.Label.create() },
        applyAttrs = {
            forId(forId)
            attrs()
        },
        content = content
    )
}

@Composable
fun Table(
    attrs: AttrBuilderContext<Tag.Table> = {},
    content: ContentBuilder<HTMLTableElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Table.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Caption(
    attrs: AttrBuilderContext<Tag.Caption> = {},
    content: ContentBuilder<HTMLTableCaptionElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Caption.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Col(
    attrs: AttrBuilderContext<Tag.Col> = {}
) {
    TagElement<Tag.Col, HTMLTableColElement>(
        elementBuilder = { ElementBuilder.Col.create() },
        applyAttrs = attrs,
        content = null
    )
}

@Composable
fun Colgroup(
    attrs: AttrBuilderContext<Tag.Colgroup> = {},
    content: ContentBuilder<HTMLTableColElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Colgroup.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tr(
    attrs: AttrBuilderContext<Tag.Tr> = {},
    content: ContentBuilder<HTMLTableRowElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Tr.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Thead(
    attrs: AttrBuilderContext<Tag.Thead> = {},
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Thead.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Th(
    attrs: AttrBuilderContext<Tag.Th> = {},
    content: ContentBuilder<HTMLTableCellElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Th.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Td(
    attrs: AttrBuilderContext<Tag.Td> = {},
    content: ContentBuilder<HTMLTableCellElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Td.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tbody(
    attrs: AttrBuilderContext<Tag.Tbody> = {},
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Tbody.create() },
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Tfoot(
    attrs: AttrBuilderContext<Tag.Tfoot> = {},
    content: ContentBuilder<HTMLTableSectionElement>? = null
) {
    TagElement(
        elementBuilder = { ElementBuilder.Tfoot.create() },
        applyAttrs = attrs,
        content = content
    )
}
