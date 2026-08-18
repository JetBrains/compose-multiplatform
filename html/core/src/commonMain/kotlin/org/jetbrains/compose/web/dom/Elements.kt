package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.web.attributes.SelectAttrsScope
import kotlinx.browser.dom.HTMLAnchorElement
import kotlinx.browser.dom.HTMLAreaElement
import kotlinx.browser.dom.HTMLAudioElement
import kotlinx.browser.dom.HTMLBRElement
import kotlinx.browser.dom.HTMLBodyElement
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
import kotlinx.browser.dom.HTMLHeadElement
import kotlinx.browser.dom.HTMLHeadingElement
import kotlinx.browser.dom.HTMLHtmlElement
import kotlinx.browser.dom.HTMLIFrameElement
import kotlinx.browser.dom.HTMLImageElement
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.HTMLLIElement
import kotlinx.browser.dom.HTMLLabelElement
import kotlinx.browser.dom.HTMLLegendElement
import kotlinx.browser.dom.HTMLLinkElement
import kotlinx.browser.dom.HTMLMapElement
import kotlinx.browser.dom.HTMLMeterElement
import kotlinx.browser.dom.HTMLMetaElement
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
import kotlinx.browser.dom.HTMLTitleElement
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

@Composable
fun Html(
    attrs: AttrBuilderContext<HTMLHtmlElement>? = null,
    content: ContentBuilder<HTMLHtmlElement>? = null,
) = TagElement<HTMLHtmlElement>("html", attrs, content)

@Composable
fun Head(
    attrs: AttrBuilderContext<HTMLHeadElement>? = null,
    content: ContentBuilder<HTMLHeadElement>? = null,
) = TagElement<HTMLHeadElement>("head", attrs, content)

@Composable
fun Body(
    attrs: AttrBuilderContext<HTMLBodyElement>? = null,
    content: ContentBuilder<HTMLBodyElement>? = null,
) = TagElement<HTMLBodyElement>("body", attrs, content)

@Composable
fun Title(
    attrs: AttrBuilderContext<HTMLTitleElement>? = null,
    content: ContentBuilder<HTMLTitleElement>? = null,
) = TagElement<HTMLTitleElement>("title", attrs, content)

@Composable
fun Meta(
    attrs: AttrBuilderContext<HTMLMetaElement>? = null,
) = TagElement<HTMLMetaElement>("meta", attrs, content = null)

@Composable
fun Link(
    attrs: AttrBuilderContext<HTMLLinkElement>? = null,
) = TagElement<HTMLLinkElement>("link", attrs, content = null)

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
 *
 * Controlled state restoration requires an actual DOM element and is skipped by
 * renderers that do not provide DOM element access, such as string rendering.
 */
@OptIn(ComposeWebInternalApi::class)
@Composable
fun <K> Input(
    type: InputType<K>,
    attrs: InputAttrsScope<K>.() -> Unit,
) {
    val context = LocalComposeHtmlContext.current

    // Changes to this key trigger controlled input state restoration in a DOM renderer.
    val keyForRestoringControlledState: MutableState<Int> = remember { mutableStateOf(0) }

    val domEffects: ContentBuilder<HTMLInputElement>? = if (context.supportsDomElementAccess) {
        {
            if (type == InputType.Radio) {
                DisposeRadioGroupEffect()
            }
            DisposableEffect(keyForRestoringControlledState.value) {
                restoreControlledInputState(inputElement = scopeElement)
                onDispose { }
            }
        }
    } else {
        null
    }

    TagElement<HTMLInputElement>(
        elementBuilder = context.elementBuilder("input"),
        applyAttrs = {
            val inputAttrsBuilder = InputAttrsScope(type, this)
            inputAttrsBuilder.type(type)
            inputAttrsBuilder.onInput {
                // Controlled state needs to be restored after every input.
                keyForRestoringControlledState.value = keyForRestoringControlledState.value + 1
            }
            inputAttrsBuilder.attrs()
        },
        content = domEffects,
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
 *
 * Textarea values are DOM property updates and are therefore omitted by renderers
 * that do not provide DOM element access, such as string rendering.
 */
@Composable
fun TextArea(
    value: String? = null,
    attrs: (TextAreaAttrsScope.() -> Unit)? = null,
) {
    val context = LocalComposeHtmlContext.current

    // If the first provided value was not null, TextArea behaves as a controlled input.
    val firstProvidedValueWasNotNull = remember { value != null }

    // Changes to this key trigger controlled textarea state restoration in a DOM renderer.
    val keyForRestoringControlledState: MutableState<Int> = remember { mutableStateOf(0) }

    val domEffects: ContentBuilder<HTMLTextAreaElement>? = if (context.supportsDomElementAccess) {
        {
            DisposableEffect(keyForRestoringControlledState.value) {
                restoreControlledTextAreaState(element = scopeElement)
                onDispose { }
            }
        }
    } else {
        null
    }

    TagElement<HTMLTextAreaElement>(
        elementBuilder = context.elementBuilder("textarea"),
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
        content = domEffects,
    )
}

@Composable
fun Area(
    attrs: AttrBuilderContext<HTMLAreaElement>? = null,
    content: ContentBuilder<HTMLAreaElement>? = null,
) = TagElement<HTMLAreaElement>("area", attrs, content)

@Composable
fun Track(
    attrs: AttrBuilderContext<HTMLTrackElement>? = null,
    content: ContentBuilder<HTMLTrackElement>? = null,
) = TagElement<HTMLTrackElement>("track", attrs, content)

@Composable
fun Embed(
    attrs: AttrBuilderContext<HTMLEmbedElement>? = null,
    content: ContentBuilder<HTMLEmbedElement>? = null,
) = TagElement<HTMLEmbedElement>("embed", attrs, content)

@Composable
fun Param(
    attrs: AttrBuilderContext<HTMLParamElement>? = null,
    content: ContentBuilder<HTMLParamElement>? = null,
) = TagElement<HTMLParamElement>("param", attrs, content)

@Composable
fun Source(
    attrs: AttrBuilderContext<HTMLSourceElement>? = null,
    content: ContentBuilder<HTMLSourceElement>? = null,
) = TagElement<HTMLSourceElement>("source", attrs, content)

@Composable
fun Br(
    attrs: AttrBuilderContext<HTMLBRElement>? = null,
) = TagElement<HTMLBRElement>("br", attrs, content = null)

@Composable
fun Hr(
    attrs: AttrBuilderContext<HTMLHRElement>? = null,
) = TagElement<HTMLHRElement>("hr", attrs, content = null)

@Composable
fun Col(
    attrs: AttrBuilderContext<HTMLTableColElement>? = null,
) = TagElement<HTMLTableColElement>("col", attrs, content = null)

@Composable
fun A(
    href: String? = null,
    attrs: AttrBuilderContext<HTMLAnchorElement>? = null,
    content: ContentBuilder<HTMLAnchorElement>? = null,
) = TagElement<HTMLAnchorElement>(
    tagName = "a",
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
) = TagElement<HTMLImageElement>(
    tagName = "img",
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
) = TagElement<HTMLFormElement>(
    tagName = "form",
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
) = TagElement<HTMLSelectElement>(
    tagName = "select",
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
) = TagElement<HTMLOptionElement>(
    tagName = "option",
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
) = TagElement<HTMLOptGroupElement>(
    tagName = "optgroup",
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
) = TagElement<HTMLLabelElement>(
    tagName = "label",
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
fun Text(value: String) {
    LocalComposeHtmlContext.current.TextElement(value)
}
