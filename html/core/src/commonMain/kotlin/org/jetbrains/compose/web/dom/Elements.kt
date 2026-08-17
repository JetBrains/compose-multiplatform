package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLAudioElement
import kotlinx.browser.dom.HTMLButtonElement
import kotlinx.browser.dom.HTMLCanvasElement
import kotlinx.browser.dom.HTMLDataListElement
import kotlinx.browser.dom.HTMLDListElement
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLFieldSetElement
import kotlinx.browser.dom.HTMLHeadingElement
import kotlinx.browser.dom.HTMLIFrameElement
import kotlinx.browser.dom.HTMLLIElement
import kotlinx.browser.dom.HTMLLegendElement
import kotlinx.browser.dom.HTMLMapElement
import kotlinx.browser.dom.HTMLMeterElement
import kotlinx.browser.dom.HTMLOListElement
import kotlinx.browser.dom.HTMLObjectElement
import kotlinx.browser.dom.HTMLOutputElement
import kotlinx.browser.dom.HTMLParagraphElement
import kotlinx.browser.dom.HTMLPictureElement
import kotlinx.browser.dom.HTMLPreElement
import kotlinx.browser.dom.HTMLProgressElement
import kotlinx.browser.dom.HTMLSpanElement
import kotlinx.browser.dom.HTMLTableCaptionElement
import kotlinx.browser.dom.HTMLTableCellElement
import kotlinx.browser.dom.HTMLTableColElement
import kotlinx.browser.dom.HTMLTableElement
import kotlinx.browser.dom.HTMLTableRowElement
import kotlinx.browser.dom.HTMLTableSectionElement
import kotlinx.browser.dom.HTMLUListElement
import kotlinx.browser.dom.HTMLVideoElement
import org.jetbrains.compose.web.attributes.AttrsScope

typealias AttrBuilderContext<T> = AttrsScope<T>.() -> Unit
typealias ContentBuilder<T> = @Composable ElementScope<T>.() -> Unit

@Composable
fun Div(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>? = null,
) = TagElement<HTMLDivElement>("div", attrs, content)

@Composable
fun Span(
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
    content: ContentBuilder<HTMLSpanElement>? = null,
) = TagElement<HTMLSpanElement>("span", attrs, content)

@Composable
fun Address(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("address", attrs, content)

@Composable
fun Article(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("article", attrs, content)

@Composable
fun Aside(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("aside", attrs, content)

@Composable
fun Header(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("header", attrs, content)

@Composable
fun Section(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("section", attrs, content)

@Composable
fun Nav(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("nav", attrs, content)

@Composable
fun Main(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("main", attrs, content)

@Composable
fun Footer(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("footer", attrs, content)

@Composable
fun H1(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement<HTMLHeadingElement>("h1", attrs, content)

@Composable
fun H2(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement<HTMLHeadingElement>("h2", attrs, content)

@Composable
fun H3(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement<HTMLHeadingElement>("h3", attrs, content)

@Composable
fun H4(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement<HTMLHeadingElement>("h4", attrs, content)

@Composable
fun H5(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement<HTMLHeadingElement>("h5", attrs, content)

@Composable
fun H6(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement<HTMLHeadingElement>("h6", attrs, content)

@Composable
fun P(
    attrs: AttrBuilderContext<HTMLParagraphElement>? = null,
    content: ContentBuilder<HTMLParagraphElement>? = null,
) = TagElement<HTMLParagraphElement>("p", attrs, content)

@Composable
fun Em(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("em", attrs, content)

@Composable
fun I(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("i", attrs, content)

@Composable
fun B(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("b", attrs, content)

@Composable
fun Small(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("small", attrs, content)

@Composable
fun Sup(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("sup", attrs, content)

@Composable
fun Sub(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("sub", attrs, content)

@Composable
fun Blockquote(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("blockquote", attrs, content)

@Composable
fun Pre(
    attrs: AttrBuilderContext<HTMLPreElement>? = null,
    content: ContentBuilder<HTMLPreElement>? = null,
) = TagElement<HTMLPreElement>("pre", attrs, content)

@Composable
fun Code(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("code", attrs, content)

@Composable
fun Ul(
    attrs: AttrBuilderContext<HTMLUListElement>? = null,
    content: ContentBuilder<HTMLUListElement>? = null,
) = TagElement<HTMLUListElement>("ul", attrs, content)

@Composable
fun Ol(
    attrs: AttrBuilderContext<HTMLOListElement>? = null,
    content: ContentBuilder<HTMLOListElement>? = null,
) = TagElement<HTMLOListElement>("ol", attrs, content)

@Composable
fun Li(
    attrs: AttrBuilderContext<HTMLLIElement>? = null,
    content: ContentBuilder<HTMLLIElement>? = null,
) = TagElement<HTMLLIElement>("li", attrs, content)

@Composable
fun DList(
    attrs: AttrBuilderContext<HTMLDListElement>? = null,
    content: ContentBuilder<HTMLDListElement>? = null,
) = TagElement<HTMLDListElement>("dl", attrs, content)

@Composable
fun DTerm(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("dt", attrs, content)

@Composable
fun DDescription(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement<HTMLElement>("dd", attrs, content)

@Composable
fun Audio(
    attrs: AttrBuilderContext<HTMLAudioElement>? = null,
    content: ContentBuilder<HTMLAudioElement>? = null,
) = TagElement<HTMLAudioElement>("audio", attrs, content)

@Composable
fun Video(
    attrs: AttrBuilderContext<HTMLVideoElement>? = null,
    content: ContentBuilder<HTMLVideoElement>? = null,
) = TagElement<HTMLVideoElement>("video", attrs, content)

@Composable
fun Picture(
    attrs: AttrBuilderContext<HTMLPictureElement>? = null,
    content: ContentBuilder<HTMLPictureElement>? = null,
) = TagElement<HTMLPictureElement>("picture", attrs, content)

@Composable
fun Canvas(
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null,
) = TagElement<HTMLCanvasElement>("canvas", attrs, content)

@Composable
fun HTMLMap(
    attrs: AttrBuilderContext<HTMLMapElement>? = null,
    content: ContentBuilder<HTMLMapElement>? = null,
) = TagElement<HTMLMapElement>("map", attrs, content)

@Composable
fun Datalist(
    attrs: AttrBuilderContext<HTMLDataListElement>? = null,
    content: ContentBuilder<HTMLDataListElement>? = null,
) = TagElement<HTMLDataListElement>("datalist", attrs, content)

@Composable
fun Fieldset(
    attrs: AttrBuilderContext<HTMLFieldSetElement>? = null,
    content: ContentBuilder<HTMLFieldSetElement>? = null,
) = TagElement<HTMLFieldSetElement>("fieldset", attrs, content)

@Composable
fun Legend(
    attrs: AttrBuilderContext<HTMLLegendElement>? = null,
    content: ContentBuilder<HTMLLegendElement>? = null,
) = TagElement<HTMLLegendElement>("legend", attrs, content)

@Composable
fun Meter(
    attrs: AttrBuilderContext<HTMLMeterElement>? = null,
    content: ContentBuilder<HTMLMeterElement>? = null,
) = TagElement<HTMLMeterElement>("meter", attrs, content)

@Composable
fun Output(
    attrs: AttrBuilderContext<HTMLOutputElement>? = null,
    content: ContentBuilder<HTMLOutputElement>? = null,
) = TagElement<HTMLOutputElement>("output", attrs, content)

@Composable
fun Progress(
    attrs: AttrBuilderContext<HTMLProgressElement>? = null,
    content: ContentBuilder<HTMLProgressElement>? = null,
) = TagElement<HTMLProgressElement>("progress", attrs, content)

@Composable
fun Iframe(
    attrs: AttrBuilderContext<HTMLIFrameElement>? = null,
    content: ContentBuilder<HTMLIFrameElement>? = null,
) = TagElement<HTMLIFrameElement>("iframe", attrs, content)

@Composable
fun Object(
    attrs: AttrBuilderContext<HTMLObjectElement>? = null,
    content: ContentBuilder<HTMLObjectElement>? = null,
) = TagElement<HTMLObjectElement>("object", attrs, content)

@Composable
fun Table(
    attrs: AttrBuilderContext<HTMLTableElement>? = null,
    content: ContentBuilder<HTMLTableElement>? = null,
) = TagElement<HTMLTableElement>("table", attrs, content)

@Composable
fun Caption(
    attrs: AttrBuilderContext<HTMLTableCaptionElement>? = null,
    content: ContentBuilder<HTMLTableCaptionElement>? = null,
) = TagElement<HTMLTableCaptionElement>("caption", attrs, content)

@Composable
fun Colgroup(
    attrs: AttrBuilderContext<HTMLTableColElement>? = null,
    content: ContentBuilder<HTMLTableColElement>? = null,
) = TagElement<HTMLTableColElement>("colgroup", attrs, content)

@Composable
fun Tr(
    attrs: AttrBuilderContext<HTMLTableRowElement>? = null,
    content: ContentBuilder<HTMLTableRowElement>? = null,
) = TagElement<HTMLTableRowElement>("tr", attrs, content)

@Composable
fun Thead(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null,
) = TagElement<HTMLTableSectionElement>("thead", attrs, content)

@Composable
fun Th(
    attrs: AttrBuilderContext<HTMLTableCellElement>? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null,
) = TagElement<HTMLTableCellElement>("th", attrs, content)

@Composable
fun Td(
    attrs: AttrBuilderContext<HTMLTableCellElement>? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null,
) = TagElement<HTMLTableCellElement>("td", attrs, content)

@Composable
fun Tbody(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null,
) = TagElement<HTMLTableSectionElement>("tbody", attrs, content)

@Composable
fun Tfoot(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null,
) = TagElement<HTMLTableSectionElement>("tfoot", attrs, content)

@Composable
fun Button(
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
    content: ContentBuilder<HTMLButtonElement>? = null,
) = TagElement<HTMLButtonElement>("button", attrs, content)

@Composable
fun Text(value: String) {
    LocalComposeHtmlContext.current.TextElement(value)
}
