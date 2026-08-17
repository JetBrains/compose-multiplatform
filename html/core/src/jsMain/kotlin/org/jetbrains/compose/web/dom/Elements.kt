package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import androidx.compose.web.attributes.SelectAttrsScope
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.attributes.builders.*
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.StyleSheetBuilder
import org.jetbrains.compose.web.css.StyleSheetBuilderImpl
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLAreaElement
import org.w3c.dom.HTMLBRElement
import org.w3c.dom.HTMLEmbedElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLHRElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.HTMLOptGroupElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLParamElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLSourceElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTableColElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.HTMLTrackElement
import org.w3c.dom.css.CSSStyleSheet

private val Area: ElementBuilder<HTMLAreaElement> = ElementBuilder.createBuilder("area")
private val Track: ElementBuilder<HTMLTrackElement> = ElementBuilder.createBuilder("track")

private val Embed: ElementBuilder<HTMLEmbedElement> = ElementBuilder.createBuilder("embed")
private val Param: ElementBuilder<HTMLParamElement> = ElementBuilder.createBuilder("param")
private val Source: ElementBuilder<HTMLSourceElement> = ElementBuilder.createBuilder("source")

private val A: ElementBuilder<HTMLAnchorElement> = ElementBuilder.createBuilder("a")
private val Input: ElementBuilder<HTMLInputElement> = ElementBuilder.createBuilder("input")

private val Br: ElementBuilder<HTMLBRElement> = ElementBuilder.createBuilder("br")

private val Img: ElementBuilder<HTMLImageElement> = ElementBuilder.createBuilder("img")
private val Form: ElementBuilder<HTMLFormElement> = ElementBuilder.createBuilder("form")

private val Select: ElementBuilder<HTMLSelectElement> = ElementBuilder.createBuilder("select")
private val Option: ElementBuilder<HTMLOptionElement> = ElementBuilder.createBuilder("option")
private val OptGroup: ElementBuilder<HTMLOptGroupElement> = ElementBuilder.createBuilder("optgroup")

private val TextArea: ElementBuilder<HTMLTextAreaElement> = ElementBuilder.createBuilder("textarea")
private val Hr: ElementBuilder<HTMLHRElement> = ElementBuilder.createBuilder("hr")
private val Label: ElementBuilder<HTMLLabelElement> = ElementBuilder.createBuilder("label")
private val Col: ElementBuilder<HTMLTableColElement> = ElementBuilder.createBuilder("col")

internal val Style: ElementBuilder<HTMLStyleElement> = ElementBuilder.createBuilder("style")

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
fun Br(attrs: AttrBuilderContext<HTMLBRElement>? = null) =
    TagElement(elementBuilder = Br, applyAttrs = attrs, content = null)

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
fun Col(
    attrs: AttrBuilderContext<HTMLTableColElement>? = null
) {
    TagElement(
        elementBuilder = Col,
        applyAttrs = attrs,
        content = null
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
