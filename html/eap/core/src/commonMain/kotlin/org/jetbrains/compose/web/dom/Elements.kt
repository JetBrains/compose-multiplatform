/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.web.attributes.SelectAttrsScope
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLAnchorElement
import kotlinx.browser.dom.HTMLAreaElement
import kotlinx.browser.dom.HTMLAudioElement
import kotlinx.browser.dom.HTMLBRElement
import kotlinx.browser.dom.HTMLButtonElement
import kotlinx.browser.dom.HTMLCanvasElement
import kotlinx.browser.dom.HTMLDataListElement
import kotlinx.browser.dom.HTMLDListElement
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLEmbedElement
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLFieldSetElement
import kotlinx.browser.dom.HTMLFormElement
import kotlinx.browser.dom.HTMLHRElement
import kotlinx.browser.dom.HTMLHeadingElement
import kotlinx.browser.dom.HTMLIFrameElement
import kotlinx.browser.dom.HTMLImageElement
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.HTMLLIElement
import kotlinx.browser.dom.HTMLLabelElement
import kotlinx.browser.dom.HTMLLegendElement
import kotlinx.browser.dom.HTMLMapElement
import kotlinx.browser.dom.HTMLMeterElement
import kotlinx.browser.dom.HTMLOListElement
import kotlinx.browser.dom.HTMLObjectElement
import kotlinx.browser.dom.HTMLOptGroupElement
import kotlinx.browser.dom.HTMLOptionElement
import kotlinx.browser.dom.HTMLOutputElement
import kotlinx.browser.dom.HTMLParagraphElement
import kotlinx.browser.dom.HTMLPictureElement
import kotlinx.browser.dom.HTMLPreElement
import kotlinx.browser.dom.HTMLProgressElement
import kotlinx.browser.dom.HTMLParamElement
import kotlinx.browser.dom.HTMLSelectElement
import kotlinx.browser.dom.HTMLSourceElement
import kotlinx.browser.dom.HTMLSpanElement
import kotlinx.browser.dom.HTMLTableCaptionElement
import kotlinx.browser.dom.HTMLTableCellElement
import kotlinx.browser.dom.HTMLTableColElement
import kotlinx.browser.dom.HTMLTableElement
import kotlinx.browser.dom.HTMLTableRowElement
import kotlinx.browser.dom.HTMLTableSectionElement
import kotlinx.browser.dom.HTMLTextAreaElement
import kotlinx.browser.dom.HTMLTrackElement
import kotlinx.browser.dom.HTMLUListElement
import kotlinx.browser.dom.HTMLVideoElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.action
import org.jetbrains.compose.web.attributes.alt
import org.jetbrains.compose.web.attributes.forId
import org.jetbrains.compose.web.attributes.href
import org.jetbrains.compose.web.attributes.label
import org.jetbrains.compose.web.attributes.multiple
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.attributes.value
import org.jetbrains.compose.web.attributes.builders.InputAttrsScope
import org.jetbrains.compose.web.attributes.builders.DisposeRadioGroupEffect
import org.jetbrains.compose.web.attributes.builders.restoreControlledInputState
import org.jetbrains.compose.web.attributes.builders.restoreControlledTextAreaState
import org.jetbrains.compose.web.attributes.builders.TextAreaAttrsScope
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

typealias AttrBuilderContext<T> = AttrsScope<T>.() -> Unit
typealias ContentBuilder<T> = @Composable ElementScope<T>.() -> Unit

private val DivElementBuilder = ElementBuilder.createBuilder<HTMLDivElement>("div")
private val SpanElementBuilder = ElementBuilder.createBuilder<HTMLSpanElement>("span")
private val AddressElementBuilder = ElementBuilder.createBuilder<HTMLElement>("address")
private val ArticleElementBuilder = ElementBuilder.createBuilder<HTMLElement>("article")
private val AsideElementBuilder = ElementBuilder.createBuilder<HTMLElement>("aside")
private val HeaderElementBuilder = ElementBuilder.createBuilder<HTMLElement>("header")
private val SectionElementBuilder = ElementBuilder.createBuilder<HTMLElement>("section")
private val NavElementBuilder = ElementBuilder.createBuilder<HTMLElement>("nav")
private val MainElementBuilder = ElementBuilder.createBuilder<HTMLElement>("main")
private val FooterElementBuilder = ElementBuilder.createBuilder<HTMLElement>("footer")
private val H1ElementBuilder = ElementBuilder.createBuilder<HTMLHeadingElement>("h1")
private val H2ElementBuilder = ElementBuilder.createBuilder<HTMLHeadingElement>("h2")
private val H3ElementBuilder = ElementBuilder.createBuilder<HTMLHeadingElement>("h3")
private val H4ElementBuilder = ElementBuilder.createBuilder<HTMLHeadingElement>("h4")
private val H5ElementBuilder = ElementBuilder.createBuilder<HTMLHeadingElement>("h5")
private val H6ElementBuilder = ElementBuilder.createBuilder<HTMLHeadingElement>("h6")
private val PElementBuilder = ElementBuilder.createBuilder<HTMLParagraphElement>("p")
private val EmElementBuilder = ElementBuilder.createBuilder<HTMLElement>("em")
private val IElementBuilder = ElementBuilder.createBuilder<HTMLElement>("i")
private val BElementBuilder = ElementBuilder.createBuilder<HTMLElement>("b")
private val SmallElementBuilder = ElementBuilder.createBuilder<HTMLElement>("small")
private val SupElementBuilder = ElementBuilder.createBuilder<HTMLElement>("sup")
private val SubElementBuilder = ElementBuilder.createBuilder<HTMLElement>("sub")
private val BlockquoteElementBuilder = ElementBuilder.createBuilder<HTMLElement>("blockquote")
private val PreElementBuilder = ElementBuilder.createBuilder<HTMLPreElement>("pre")
private val CodeElementBuilder = ElementBuilder.createBuilder<HTMLElement>("code")
private val UlElementBuilder = ElementBuilder.createBuilder<HTMLUListElement>("ul")
private val OlElementBuilder = ElementBuilder.createBuilder<HTMLOListElement>("ol")
private val LiElementBuilder = ElementBuilder.createBuilder<HTMLLIElement>("li")
private val DListElementBuilder = ElementBuilder.createBuilder<HTMLDListElement>("dl")
private val DTermElementBuilder = ElementBuilder.createBuilder<HTMLElement>("dt")
private val DDescriptionElementBuilder = ElementBuilder.createBuilder<HTMLElement>("dd")
private val AudioElementBuilder = ElementBuilder.createBuilder<HTMLAudioElement>("audio")
private val VideoElementBuilder = ElementBuilder.createBuilder<HTMLVideoElement>("video")
private val PictureElementBuilder = ElementBuilder.createBuilder<HTMLPictureElement>("picture")
private val CanvasElementBuilder = ElementBuilder.createBuilder<HTMLCanvasElement>("canvas")
private val MapElementBuilder = ElementBuilder.createBuilder<HTMLMapElement>("map")
private val DatalistElementBuilder = ElementBuilder.createBuilder<HTMLDataListElement>("datalist")
private val FieldsetElementBuilder = ElementBuilder.createBuilder<HTMLFieldSetElement>("fieldset")
private val LegendElementBuilder = ElementBuilder.createBuilder<HTMLLegendElement>("legend")
private val MeterElementBuilder = ElementBuilder.createBuilder<HTMLMeterElement>("meter")
private val OutputElementBuilder = ElementBuilder.createBuilder<HTMLOutputElement>("output")
private val ProgressElementBuilder = ElementBuilder.createBuilder<HTMLProgressElement>("progress")
private val IframeElementBuilder = ElementBuilder.createBuilder<HTMLIFrameElement>("iframe")
private val ObjectElementBuilder = ElementBuilder.createBuilder<HTMLObjectElement>("object")
private val TableElementBuilder = ElementBuilder.createBuilder<HTMLTableElement>("table")
private val CaptionElementBuilder = ElementBuilder.createBuilder<HTMLTableCaptionElement>("caption")
private val ColgroupElementBuilder = ElementBuilder.createBuilder<HTMLTableColElement>("colgroup")
private val TrElementBuilder = ElementBuilder.createBuilder<HTMLTableRowElement>("tr")
private val TheadElementBuilder = ElementBuilder.createBuilder<HTMLTableSectionElement>("thead")
private val ThElementBuilder = ElementBuilder.createBuilder<HTMLTableCellElement>("th")
private val TdElementBuilder = ElementBuilder.createBuilder<HTMLTableCellElement>("td")
private val TbodyElementBuilder = ElementBuilder.createBuilder<HTMLTableSectionElement>("tbody")
private val TfootElementBuilder = ElementBuilder.createBuilder<HTMLTableSectionElement>("tfoot")
private val ButtonElementBuilder = ElementBuilder.createBuilder<HTMLButtonElement>("button")
private val InputElementBuilder = ElementBuilder.createBuilder<HTMLInputElement>("input")
private val TextAreaElementBuilder = ElementBuilder.createBuilder<HTMLTextAreaElement>("textarea")
private val AreaElementBuilder = ElementBuilder.createBuilder<HTMLAreaElement>("area")
private val TrackElementBuilder = ElementBuilder.createBuilder<HTMLTrackElement>("track")
private val EmbedElementBuilder = ElementBuilder.createBuilder<HTMLEmbedElement>("embed")
private val ParamElementBuilder = ElementBuilder.createBuilder<HTMLParamElement>("param")
private val SourceElementBuilder = ElementBuilder.createBuilder<HTMLSourceElement>("source")
private val BrElementBuilder = ElementBuilder.createBuilder<HTMLBRElement>("br")
private val HrElementBuilder = ElementBuilder.createBuilder<HTMLHRElement>("hr")
private val ColElementBuilder = ElementBuilder.createBuilder<HTMLTableColElement>("col")
private val AnchorElementBuilder = ElementBuilder.createBuilder<HTMLAnchorElement>("a")
private val ImgElementBuilder = ElementBuilder.createBuilder<HTMLImageElement>("img")
private val FormElementBuilder = ElementBuilder.createBuilder<HTMLFormElement>("form")
private val SelectElementBuilder = ElementBuilder.createBuilder<HTMLSelectElement>("select")
private val OptionElementBuilder = ElementBuilder.createBuilder<HTMLOptionElement>("option")
private val OptGroupElementBuilder = ElementBuilder.createBuilder<HTMLOptGroupElement>("optgroup")
private val LabelElementBuilder = ElementBuilder.createBuilder<HTMLLabelElement>("label")

@Composable
fun Div(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>? = null,
) = TagElement(DivElementBuilder, attrs, content)

@Composable
fun Span(
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
    content: ContentBuilder<HTMLSpanElement>? = null,
) = TagElement(SpanElementBuilder, attrs, content)

@Composable
fun Address(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(AddressElementBuilder, attrs, content)

@Composable
fun Article(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(ArticleElementBuilder, attrs, content)

@Composable
fun Aside(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(AsideElementBuilder, attrs, content)

@Composable
fun Header(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(HeaderElementBuilder, attrs, content)

@Composable
fun Section(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(SectionElementBuilder, attrs, content)

@Composable
fun Nav(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(NavElementBuilder, attrs, content)

@Composable
fun Main(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(MainElementBuilder, attrs, content)

@Composable
fun Footer(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(FooterElementBuilder, attrs, content)

@Composable
fun H1(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement(H1ElementBuilder, attrs, content)

@Composable
fun H2(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement(H2ElementBuilder, attrs, content)

@Composable
fun H3(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement(H3ElementBuilder, attrs, content)

@Composable
fun H4(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement(H4ElementBuilder, attrs, content)

@Composable
fun H5(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement(H5ElementBuilder, attrs, content)

@Composable
fun H6(
    attrs: AttrBuilderContext<HTMLHeadingElement>? = null,
    content: ContentBuilder<HTMLHeadingElement>? = null,
) = TagElement(H6ElementBuilder, attrs, content)

@Composable
fun P(
    attrs: AttrBuilderContext<HTMLParagraphElement>? = null,
    content: ContentBuilder<HTMLParagraphElement>? = null,
) = TagElement(PElementBuilder, attrs, content)

@Composable
fun Em(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(EmElementBuilder, attrs, content)

@Composable
fun I(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(IElementBuilder, attrs, content)

@Composable
fun B(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(BElementBuilder, attrs, content)

@Composable
fun Small(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(SmallElementBuilder, attrs, content)

@Composable
fun Sup(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(SupElementBuilder, attrs, content)

@Composable
fun Sub(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(SubElementBuilder, attrs, content)

@Composable
fun Blockquote(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(BlockquoteElementBuilder, attrs, content)

@Composable
fun Pre(
    attrs: AttrBuilderContext<HTMLPreElement>? = null,
    content: ContentBuilder<HTMLPreElement>? = null,
) = TagElement(PreElementBuilder, attrs, content)

@Composable
fun Code(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(CodeElementBuilder, attrs, content)

@Composable
fun Ul(
    attrs: AttrBuilderContext<HTMLUListElement>? = null,
    content: ContentBuilder<HTMLUListElement>? = null,
) = TagElement(UlElementBuilder, attrs, content)

@Composable
fun Ol(
    attrs: AttrBuilderContext<HTMLOListElement>? = null,
    content: ContentBuilder<HTMLOListElement>? = null,
) = TagElement(OlElementBuilder, attrs, content)

@Composable
fun Li(
    attrs: AttrBuilderContext<HTMLLIElement>? = null,
    content: ContentBuilder<HTMLLIElement>? = null,
) = TagElement(LiElementBuilder, attrs, content)

@Composable
fun DList(
    attrs: AttrBuilderContext<HTMLDListElement>? = null,
    content: ContentBuilder<HTMLDListElement>? = null,
) = TagElement(DListElementBuilder, attrs, content)

@Composable
fun DTerm(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(DTermElementBuilder, attrs, content)

@Composable
fun DDescription(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) = TagElement(DDescriptionElementBuilder, attrs, content)

@Composable
fun Audio(
    attrs: AttrBuilderContext<HTMLAudioElement>? = null,
    content: ContentBuilder<HTMLAudioElement>? = null,
) = TagElement(AudioElementBuilder, attrs, content)

@Composable
fun Video(
    attrs: AttrBuilderContext<HTMLVideoElement>? = null,
    content: ContentBuilder<HTMLVideoElement>? = null,
) = TagElement(VideoElementBuilder, attrs, content)

@Composable
fun Picture(
    attrs: AttrBuilderContext<HTMLPictureElement>? = null,
    content: ContentBuilder<HTMLPictureElement>? = null,
) = TagElement(PictureElementBuilder, attrs, content)

@Composable
fun Canvas(
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: ContentBuilder<HTMLCanvasElement>? = null,
) = TagElement(CanvasElementBuilder, attrs, content)

@Composable
fun HTMLMap(
    attrs: AttrBuilderContext<HTMLMapElement>? = null,
    content: ContentBuilder<HTMLMapElement>? = null,
) = TagElement(MapElementBuilder, attrs, content)

@Composable
fun Datalist(
    attrs: AttrBuilderContext<HTMLDataListElement>? = null,
    content: ContentBuilder<HTMLDataListElement>? = null,
) = TagElement(DatalistElementBuilder, attrs, content)

@Composable
fun Fieldset(
    attrs: AttrBuilderContext<HTMLFieldSetElement>? = null,
    content: ContentBuilder<HTMLFieldSetElement>? = null,
) = TagElement(FieldsetElementBuilder, attrs, content)

@Composable
fun Legend(
    attrs: AttrBuilderContext<HTMLLegendElement>? = null,
    content: ContentBuilder<HTMLLegendElement>? = null,
) = TagElement(LegendElementBuilder, attrs, content)

@Composable
fun Meter(
    attrs: AttrBuilderContext<HTMLMeterElement>? = null,
    content: ContentBuilder<HTMLMeterElement>? = null,
) = TagElement(MeterElementBuilder, attrs, content)

@Composable
fun Output(
    attrs: AttrBuilderContext<HTMLOutputElement>? = null,
    content: ContentBuilder<HTMLOutputElement>? = null,
) = TagElement(OutputElementBuilder, attrs, content)

@Composable
fun Progress(
    attrs: AttrBuilderContext<HTMLProgressElement>? = null,
    content: ContentBuilder<HTMLProgressElement>? = null,
) = TagElement(ProgressElementBuilder, attrs, content)

@Composable
fun Iframe(
    attrs: AttrBuilderContext<HTMLIFrameElement>? = null,
    content: ContentBuilder<HTMLIFrameElement>? = null,
) = TagElement(IframeElementBuilder, attrs, content)

@Composable
fun Object(
    attrs: AttrBuilderContext<HTMLObjectElement>? = null,
    content: ContentBuilder<HTMLObjectElement>? = null,
) = TagElement(ObjectElementBuilder, attrs, content)

@Composable
fun Table(
    attrs: AttrBuilderContext<HTMLTableElement>? = null,
    content: ContentBuilder<HTMLTableElement>? = null,
) = TagElement(TableElementBuilder, attrs, content)

@Composable
fun Caption(
    attrs: AttrBuilderContext<HTMLTableCaptionElement>? = null,
    content: ContentBuilder<HTMLTableCaptionElement>? = null,
) = TagElement(CaptionElementBuilder, attrs, content)

@Composable
fun Colgroup(
    attrs: AttrBuilderContext<HTMLTableColElement>? = null,
    content: ContentBuilder<HTMLTableColElement>? = null,
) = TagElement(ColgroupElementBuilder, attrs, content)

@Composable
fun Tr(
    attrs: AttrBuilderContext<HTMLTableRowElement>? = null,
    content: ContentBuilder<HTMLTableRowElement>? = null,
) = TagElement(TrElementBuilder, attrs, content)

@Composable
fun Thead(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null,
) = TagElement(TheadElementBuilder, attrs, content)

@Composable
fun Th(
    attrs: AttrBuilderContext<HTMLTableCellElement>? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null,
) = TagElement(ThElementBuilder, attrs, content)

@Composable
fun Td(
    attrs: AttrBuilderContext<HTMLTableCellElement>? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null,
) = TagElement(TdElementBuilder, attrs, content)

@Composable
fun Tbody(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null,
) = TagElement(TbodyElementBuilder, attrs, content)

@Composable
fun Tfoot(
    attrs: AttrBuilderContext<HTMLTableSectionElement>? = null,
    content: ContentBuilder<HTMLTableSectionElement>? = null,
) = TagElement(TfootElementBuilder, attrs, content)

@Composable
fun Button(
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
    content: ContentBuilder<HTMLButtonElement>? = null,
) = TagElement(ButtonElementBuilder, attrs, content)

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
    attrs: InputAttrsScope<K>.() -> Unit,
) {
    // Changes to this key trigger controlled input state restoration.
    val keyForRestoringControlledState: MutableState<Int> = remember { mutableStateOf(0) }

    TagElement(
        elementBuilder = InputElementBuilder,
        applyAttrs = {
            val inputAttrsBuilder = InputAttrsScope(type, this)
            inputAttrsBuilder.type(type)
            inputAttrsBuilder.onInput {
                // Controlled state needs to be restored after every input.
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
        },
    )
}

@Composable
fun <K> Input(type: InputType<K>) {
    Input(type) {}
}

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
    attrs: (TextAreaAttrsScope.() -> Unit)? = null,
) {
    // If the first provided value was not null, TextArea behaves as a controlled input.
    val firstProvidedValueWasNotNull = remember { value != null }

    // Changes to this key trigger controlled textarea state restoration.
    val keyForRestoringControlledState: MutableState<Int> = remember { mutableStateOf(0) }

    TagElement(
        elementBuilder = TextAreaElementBuilder,
        applyAttrs = {
            val textAreaAttrsBuilder = TextAreaAttrsScope(this)
            textAreaAttrsBuilder.onInput {
                // Controlled state needs to be restored after every input.
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
        },
    )
}

@Composable
fun Area(
    attrs: AttrBuilderContext<HTMLAreaElement>? = null,
    content: ContentBuilder<HTMLAreaElement>? = null,
) = TagElement(AreaElementBuilder, attrs, content)

@Composable
fun Track(
    attrs: AttrBuilderContext<HTMLTrackElement>? = null,
    content: ContentBuilder<HTMLTrackElement>? = null,
) = TagElement(TrackElementBuilder, attrs, content)

@Composable
fun Embed(
    attrs: AttrBuilderContext<HTMLEmbedElement>? = null,
    content: ContentBuilder<HTMLEmbedElement>? = null,
) = TagElement(EmbedElementBuilder, attrs, content)

@Composable
fun Param(
    attrs: AttrBuilderContext<HTMLParamElement>? = null,
    content: ContentBuilder<HTMLParamElement>? = null,
) = TagElement(ParamElementBuilder, attrs, content)

@Composable
fun Source(
    attrs: AttrBuilderContext<HTMLSourceElement>? = null,
    content: ContentBuilder<HTMLSourceElement>? = null,
) = TagElement(SourceElementBuilder, attrs, content)

@Composable
fun Br(
    attrs: AttrBuilderContext<HTMLBRElement>? = null,
) = TagElement(BrElementBuilder, attrs, content = null)

@Composable
fun Hr(
    attrs: AttrBuilderContext<HTMLHRElement>? = null,
) = TagElement(HrElementBuilder, attrs, content = null)

@Composable
fun Col(
    attrs: AttrBuilderContext<HTMLTableColElement>? = null,
) = TagElement(ColElementBuilder, attrs, content = null)

@Composable
fun A(
    href: String? = null,
    attrs: AttrBuilderContext<HTMLAnchorElement>? = null,
    content: ContentBuilder<HTMLAnchorElement>? = null,
) = TagElement(
    elementBuilder = AnchorElementBuilder,
    applyAttrs = {
        if (href != null) {
            this.href(href)
        }
        if (attrs != null) {
            attrs()
        }
    },
    content = content,
)

@Composable
fun Img(
    src: String,
    alt: String = "",
    attrs: AttrBuilderContext<HTMLImageElement>? = null,
) = TagElement(
    elementBuilder = ImgElementBuilder,
    applyAttrs = {
        src(src).alt(alt)
        if (attrs != null) {
            attrs()
        }
    },
    content = null,
)

@Composable
fun Form(
    action: String? = null,
    attrs: AttrBuilderContext<HTMLFormElement>? = null,
    content: ContentBuilder<HTMLFormElement>? = null,
) = TagElement(
    elementBuilder = FormElementBuilder,
    applyAttrs = {
        if (!action.isNullOrEmpty()) {
            action(action)
        }
        if (attrs != null) {
            attrs()
        }
    },
    content = content,
)

@Composable
fun Select(
    attrs: (SelectAttrsScope.() -> Unit)? = null,
    multiple: Boolean = false,
    content: ContentBuilder<HTMLSelectElement>? = null,
) = TagElement(
    elementBuilder = SelectElementBuilder,
    applyAttrs = {
        if (multiple) {
            multiple()
        }
        if (attrs != null) {
            SelectAttrsScope(this).attrs()
        }
    },
    content = content,
)

@Composable
fun Option(
    value: String,
    attrs: AttrBuilderContext<HTMLOptionElement>? = null,
    content: ContentBuilder<HTMLOptionElement>? = null,
) = TagElement(
    elementBuilder = OptionElementBuilder,
    applyAttrs = {
        value(value)
        if (attrs != null) {
            attrs()
        }
    },
    content = content,
)

@Composable
fun OptGroup(
    label: String,
    attrs: AttrBuilderContext<HTMLOptGroupElement>? = null,
    content: ContentBuilder<HTMLOptGroupElement>? = null,
) = TagElement(
    elementBuilder = OptGroupElementBuilder,
    applyAttrs = {
        label(label)
        if (attrs != null) {
            attrs()
        }
    },
    content = content,
)

@Composable
fun Label(
    forId: String? = null,
    attrs: AttrBuilderContext<HTMLLabelElement>? = null,
    content: ContentBuilder<HTMLLabelElement>? = null,
) = TagElement(
    elementBuilder = LabelElementBuilder,
    applyAttrs = {
        if (forId != null) {
            forId(forId)
        }
        if (attrs != null) {
            attrs()
        }
    },
    content = content,
)

@Composable
expect fun Text(value: String)
