package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.attributes.builders.*
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.StyleSheetBuilder
import org.jetbrains.compose.web.css.StyleSheetBuilderImpl
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.css.CSSStyleSheet

private val Input: ElementBuilder<HTMLInputElement> = ElementBuilder.createBuilder("input")

private val TextArea: ElementBuilder<HTMLTextAreaElement> = ElementBuilder.createBuilder("textarea")

internal val Style: ElementBuilder<HTMLStyleElement> = ElementBuilder.createBuilder("style")

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
