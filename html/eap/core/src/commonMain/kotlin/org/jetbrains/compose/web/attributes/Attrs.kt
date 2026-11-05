/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.dom.LowerableAttribute
import org.jetbrains.compose.web.dom.htmlAutoCompleteValue
import org.jetbrains.compose.web.attributes.builders.saveControlledInputState
import org.jetbrains.compose.web.events.SyntheticSubmitEvent
import kotlinx.browser.dom.HTMLAnchorElement
import kotlinx.browser.dom.HTMLButtonElement
import kotlinx.browser.dom.HTMLCanvasElement
import kotlinx.browser.dom.HTMLFormElement
import kotlinx.browser.dom.HTMLImageElement
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.HTMLLabelElement
import kotlinx.browser.dom.HTMLLinkElement
import kotlinx.browser.dom.HTMLScriptElement
import kotlinx.browser.dom.HTMLOptGroupElement
import kotlinx.browser.dom.HTMLOptionElement
import kotlinx.browser.dom.HTMLSelectElement
import kotlinx.browser.dom.HTMLTableCellElement
import kotlinx.browser.dom.HTMLTableColElement
import kotlinx.browser.dom.HTMLTextAreaElement
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import kotlin.jvm.JvmName

@LowerableAttribute
fun AttrsScope<HTMLAnchorElement>.href(value: String) =
    attr("href", value)

@LowerableAttribute(conversion = "targetStr", defaultValue = ["_self"])
fun AttrsScope<HTMLAnchorElement>.target(value: ATarget = ATarget.Self) =
    attr("target", value.targetStr)

@LowerableAttribute(conversion = "relStr")
fun AttrsScope<HTMLAnchorElement>.rel(value: ARel) =
    attr("rel", value.relStr)

@Deprecated(
    message = "Use `rel` instead.",
    replaceWith = ReplaceWith("rel(value)", "org.jetbrains.compose.web.attributes.rel"),
    level = DeprecationLevel.WARNING,
)
fun AttrsScope<HTMLAnchorElement>.ref(value: ARel) =
    rel(value)

@LowerableAttribute
fun AttrsScope<HTMLAnchorElement>.ping(value: String) =
    attr("ping", value)

fun AttrsScope<HTMLAnchorElement>.ping(vararg urls: String) =
    attr("ping", urls.joinToString(" "))

@LowerableAttribute
fun AttrsScope<HTMLAnchorElement>.hreflang(value: String) =
    attr("hreflang", value)

@LowerableAttribute(defaultValue = [""])
fun AttrsScope<HTMLAnchorElement>.download(value: String = "") =
    attr("download", value)

/* Link attributes */

@JvmName("linkHref")
@LowerableAttribute
fun AttrsScope<HTMLLinkElement>.href(value: String) =
    attr("href", value)

fun AttrsScope<HTMLLinkElement>.rel(
    value: LinkRel,
    vararg additionalValues: LinkRel,
) = attr(
    "rel",
    listOf(value, *additionalValues).joinToString(" ") { it.relStr },
)

@LowerableAttribute
fun AttrsScope<HTMLLinkElement>.type(value: String) =
    attr("type", value)

/* Script attributes */

@JvmName("scriptSrc")
@LowerableAttribute
fun AttrsScope<HTMLScriptElement>.src(value: String) =
    attr("src", value)

@JvmName("scriptType")
@LowerableAttribute(conversion = "typeStr")
fun AttrsScope<HTMLScriptElement>.type(value: ScriptType) =
    attr("type", value.typeStr)

/* Button attributes */

@JvmName("buttonAutoFocus")
@LowerableAttribute
fun AttrsScope<HTMLButtonElement>.autoFocus() =
    attr("autofocus", "")

@JvmName("buttonDisabled")
@LowerableAttribute
fun AttrsScope<HTMLButtonElement>.disabled() =
    attr("disabled", "")

@JvmName("buttonForm")
@LowerableAttribute
fun AttrsScope<HTMLButtonElement>.form(formId: String) =
    attr("form", formId)

@JvmName("buttonFormAction")
@LowerableAttribute
fun AttrsScope<HTMLButtonElement>.formAction(url: String) =
    attr("formaction", url)

@LowerableAttribute(conversion = "typeStr")
fun AttrsScope<HTMLButtonElement>.formEncType(value: ButtonFormEncType) =
    attr("formenctype", value.typeStr)

@LowerableAttribute(conversion = "methodStr")
fun AttrsScope<HTMLButtonElement>.formMethod(value: ButtonFormMethod) =
    attr("formmethod", value.methodStr)

@JvmName("buttonFormNoValidate")
@LowerableAttribute
fun AttrsScope<HTMLButtonElement>.formNoValidate() =
    attr("formnovalidate", "")

@LowerableAttribute(conversion = "targetStr")
fun AttrsScope<HTMLButtonElement>.formTarget(value: ButtonFormTarget) =
    attr("formtarget", value.targetStr)

@JvmName("buttonName")
@LowerableAttribute
fun AttrsScope<HTMLButtonElement>.name(value: String) =
    attr("name", value)

@LowerableAttribute(conversion = "str")
fun AttrsScope<HTMLButtonElement>.type(value: ButtonType) =
    attr("type", value.str)

@JvmName("buttonValue")
@LowerableAttribute
fun AttrsScope<HTMLButtonElement>.value(value: String) =
    attr("value", value)

/* Form attributes */

@LowerableAttribute
fun AttrsScope<HTMLFormElement>.action(value: String) =
    attr("action", value)

@LowerableAttribute("accept-charset")
fun AttrsScope<HTMLFormElement>.acceptCharset(value: String) =
    attr("accept-charset", value)

@LowerableAttribute(conversion = "onOff", defaultValue = ["on"])
fun AttrsScope<HTMLFormElement>.autoComplete(value: Boolean = true) =
    attr("autocomplete", htmlAutoCompleteValue(value))

@LowerableAttribute(conversion = "typeStr")
fun AttrsScope<HTMLFormElement>.encType(value: FormEncType) =
    attr("enctype", value.typeStr)

@LowerableAttribute(conversion = "methodStr")
fun AttrsScope<HTMLFormElement>.method(value: FormMethod) =
    attr("method", value.methodStr)

@LowerableAttribute
fun AttrsScope<HTMLFormElement>.noValidate() =
    attr("novalidate", "")

@LowerableAttribute(conversion = "targetStr")
fun AttrsScope<HTMLFormElement>.target(value: FormTarget) =
    attr("target", value.targetStr)

@OptIn(ComposeWebInternalApi::class)
fun AttrsScope<HTMLFormElement>.onSubmit(
    listener: (SyntheticSubmitEvent) -> Unit
) {
    registerEventListener(SubmitEventListener(EventsListenerScope.SUBMIT, listener))
}

@OptIn(ComposeWebInternalApi::class)
fun AttrsScope<HTMLFormElement>.onReset(
    listener: (SyntheticSubmitEvent) -> Unit
) {
    registerEventListener(SubmitEventListener(EventsListenerScope.RESET, listener))
}

/* Input attributes */

@LowerableAttribute(conversion = "typeStr")
fun AttrsScope<HTMLInputElement>.type(value: InputType<*>) =
    attr("type", value.typeStr)

@LowerableAttribute
fun AttrsScope<HTMLInputElement>.accept(value: String) =
    attr("accept", value) // type: file only

// JVM erases AttrsScope's element type, so element-specific extensions with otherwise identical signatures need distinct JVM names.
// Web targets do not use JVM bytecode signatures, so they have no equivalent signature clash.
@JvmName("inputAlt")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.alt(value: String) =
    attr("alt", value) // type: image only

@JvmName("inputAutoComplete")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.toString())

@JvmName("inputAutoFocus")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.autoFocus() =
    attr("autofocus", "")

@LowerableAttribute
fun AttrsScope<HTMLInputElement>.capture(value: String) =
    attr("capture", value) // type: file only

@LowerableAttribute
fun AttrsScope<HTMLInputElement>.dirName(value: String) =
    attr("dirname", value) // text, search

@JvmName("inputDisabled")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.disabled() =
    attr("disabled", "")

@JvmName("inputForm")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.form(id: String) =
    attr("form", id)

@JvmName("inputFormAction")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.formAction(url: String) =
    attr("formaction", url)

@LowerableAttribute(conversion = "typeStr")
fun AttrsScope<HTMLInputElement>.formEncType(value: InputFormEncType) =
    attr("formenctype", value.typeStr)

@LowerableAttribute(conversion = "methodStr")
fun AttrsScope<HTMLInputElement>.formMethod(value: InputFormMethod) =
    attr("formmethod", value.methodStr)

@JvmName("inputFormNoValidate")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.formNoValidate() =
    attr("formnovalidate", "")

@LowerableAttribute(conversion = "targetStr")
fun AttrsScope<HTMLInputElement>.formTarget(value: InputFormTarget) =
    attr("formtarget", value.targetStr)

@JvmName("inputHeight")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.height(value: Int) =
    attr("height", value.toString()) // image only

@JvmName("inputWidth")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.width(value: Int) =
    attr("width", value.toString()) // image only

@JvmName("canvasWidth")
@LowerableAttribute
fun AttrsScope<HTMLCanvasElement>.width(value: Int) =
    attr("width", value.toString())

@JvmName("canvasHeight")
@LowerableAttribute
fun AttrsScope<HTMLCanvasElement>.height(value: Int) =
    attr("height", value.toString())

@LowerableAttribute
fun AttrsScope<HTMLInputElement>.list(dataListId: String) =
    attr("list", dataListId)

@LowerableAttribute
fun AttrsScope<HTMLInputElement>.max(value: String) =
    attr("max", value)

@JvmName("inputMaxLength")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

@LowerableAttribute
fun AttrsScope<HTMLInputElement>.min(value: String) =
    attr("min", value)

@JvmName("inputMinLength")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.minLength(value: Int) =
    attr("minlength", value.toString())

@JvmName("inputMultiple")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.multiple() =
    attr("multiple", "")

@JvmName("inputName")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.name(value: String) =
    attr("name", value)

@LowerableAttribute
fun AttrsScope<HTMLInputElement>.pattern(value: String) =
    attr("pattern", value)

@JvmName("inputPlaceholder")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.placeholder(value: String) =
    attr("placeholder", value)

@JvmName("inputReadOnly")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.readOnly() =
    attr("readonly", "")

@Deprecated(
    message = "Please use `required()` without parameters. Use if..else.. if conditional behaviour required.",
    replaceWith = ReplaceWith("required()", "org.jetbrains.compose.web.attributes.required"),
    level = DeprecationLevel.WARNING
)
@JvmName("inputRequired")
@LowerableAttribute(defaultValue = ["true"])
fun AttrsScope<HTMLInputElement>.required(value: Boolean = true) =
    attr("required", value.toString())

@JvmName("inputRequired")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.required() =
    attr("required", "")

@JvmName("inputSize")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.size(value: Int) =
    attr("size", value.toString())

@JvmName("inputSrc")
@LowerableAttribute
fun AttrsScope<HTMLInputElement>.src(value: String) =
    attr("src", value) // image only

@LowerableAttribute
fun AttrsScope<HTMLInputElement>.step(value: Number) =
    attr("step", value.toString()) // numeric types only

/* Option attributes */

@JvmName("optionValue")
@LowerableAttribute
fun AttrsScope<HTMLOptionElement>.value(value: String) =
    attr("value", value)

@JvmName("optionDisabled")
@LowerableAttribute
fun AttrsScope<HTMLOptionElement>.disabled() =
    attr("disabled", "")

@LowerableAttribute
fun AttrsScope<HTMLOptionElement>.selected() =
    attr("selected", "")

@JvmName("optionLabel")
@LowerableAttribute
fun AttrsScope<HTMLOptionElement>.label(value: String) =
    attr("label", value)

/* Select attributes */

@JvmName("selectAutoComplete")
@LowerableAttribute
fun AttrsScope<HTMLSelectElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.toString())

@LowerableAttribute
fun AttrsScope<HTMLSelectElement>.autofocus() =
    attr("autofocus", "")

@JvmName("selectDisabled")
@LowerableAttribute
fun AttrsScope<HTMLSelectElement>.disabled() =
    attr("disabled", "")

@JvmName("selectForm")
@LowerableAttribute
fun AttrsScope<HTMLSelectElement>.form(formId: String) =
    attr("form", formId)

@JvmName("selectMultiple")
@LowerableAttribute
fun AttrsScope<HTMLSelectElement>.multiple() =
    attr("multiple", "")

@JvmName("selectName")
@LowerableAttribute
fun AttrsScope<HTMLSelectElement>.name(value: String) =
    attr("name", value)

@JvmName("selectRequired")
@LowerableAttribute
fun AttrsScope<HTMLSelectElement>.required() =
    attr("required", "")

@JvmName("selectSize")
@LowerableAttribute
fun AttrsScope<HTMLSelectElement>.size(numberOfRows: Int) =
    attr("size", numberOfRows.toString())

/* OptGroup attributes */

@JvmName("optGroupLabel")
@LowerableAttribute
fun AttrsScope<HTMLOptGroupElement>.label(value: String) =
    attr("label", value)

@JvmName("optGroupDisabled")
@LowerableAttribute
fun AttrsScope<HTMLOptGroupElement>.disabled() =
    attr("disabled", "")

/* TextArea attributes */

@JvmName("textAreaAutoComplete")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.toString())

@JvmName("textAreaAutoFocus")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.autoFocus() =
    attr("autofocus", "")

@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.cols(value: Int) =
    attr("cols", value.toString())

@JvmName("textAreaDisabled")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.disabled() =
    attr("disabled", "")

@JvmName("textAreaForm")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.form(formId: String) =
    attr("form", formId)

@JvmName("textAreaMaxLength")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

@JvmName("textAreaMinLength")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.minLength(value: Int) =
    attr("minlength", value.toString())

@JvmName("textAreaName")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.name(value: String) =
    attr("name", value)

@JvmName("textAreaPlaceholder")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.placeholder(value: String) =
    attr("placeholder", value)

@JvmName("textAreaReadOnly")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.readOnly() =
    attr("readonly", "")

@JvmName("textAreaRequired")
@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.required() =
    attr("required", "")

@LowerableAttribute
fun AttrsScope<HTMLTextAreaElement>.rows(value: Int) =
    attr("rows", value.toString())

@LowerableAttribute(conversion = "str")
fun AttrsScope<HTMLTextAreaElement>.wrap(value: TextAreaWrap) =
    attr("wrap", value.str)

/* Img attributes */

@JvmName("imageSrc")
@LowerableAttribute
fun AttrsScope<HTMLImageElement>.src(value: String): AttrsScope<HTMLImageElement> =
    attr("src", value)

@JvmName("imageAlt")
@LowerableAttribute
fun AttrsScope<HTMLImageElement>.alt(value: String): AttrsScope<HTMLImageElement> =
    attr("alt", value)


internal val setInputValue: (HTMLInputElement, String) -> Unit = { e, v ->
    if (v != e.value) {
        e.value = v
    }
    saveControlledInputState(e, v)
}

internal val setTextAreaValue: (HTMLTextAreaElement, String) -> Unit = { e, v ->
    if (v != e.value) {
        e.value = v
    }
    saveControlledInputState(e, v)
}

internal val setTextAreaDefaultValue: (HTMLTextAreaElement, String) -> Unit = { e, v ->
    if (v != e.defaultValue) {
        e.defaultValue = v
    }
}

internal val setCheckedValue: (HTMLInputElement, Boolean) -> Unit = { e, v ->
    e.checked = v
    saveControlledInputState(e, v)
}

/* Img attributes */
@LowerableAttribute("for")
fun AttrsScope<HTMLLabelElement>.forId(value: String): AttrsScope<HTMLLabelElement> =
    attr("for", value)

/* Table attributes */
@LowerableAttribute
fun AttrsScope<HTMLTableColElement>.span(value: Int): AttrsScope<HTMLTableColElement> =
    attr("span", value.toString())

@LowerableAttribute(conversion = "str")
fun AttrsScope<HTMLTableCellElement>.scope(value: Scope): AttrsScope<HTMLTableCellElement> =
    attr("scope", value.str)

@LowerableAttribute
fun AttrsScope<HTMLTableCellElement>.colspan(value: Int): AttrsScope<HTMLTableCellElement> =
    attr("colspan", value.toString())

@LowerableAttribute
fun AttrsScope<HTMLTableCellElement>.rowspan(value: Int): AttrsScope<HTMLTableCellElement> =
    attr("rowspan", value.toString())
