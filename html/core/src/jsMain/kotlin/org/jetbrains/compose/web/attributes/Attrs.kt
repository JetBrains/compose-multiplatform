package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.attributes.builders.saveControlledInputState
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.events.SyntheticSubmitEvent
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.HTMLOptGroupElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTableCellElement
import org.w3c.dom.HTMLTableColElement
import org.w3c.dom.HTMLTextAreaElement

fun AttrsScope<HTMLAnchorElement>.href(value: String) =
    attr("href", value)

fun AttrsScope<HTMLAnchorElement>.target(value: ATarget = ATarget.Self) =
    attr("target", value.targetStr)

fun AttrsScope<HTMLAnchorElement>.ref(value: ARel) =
    attr("rel", value.relStr)

fun AttrsScope<HTMLAnchorElement>.ping(value: String) =
    attr("ping", value)

fun AttrsScope<HTMLAnchorElement>.ping(vararg urls: String) =
    attr("ping", urls.joinToString(" "))

fun AttrsScope<HTMLAnchorElement>.hreflang(value: String) =
    attr("hreflang", value)

fun AttrsScope<HTMLAnchorElement>.download(value: String = "") =
    attr("download", value)

/* Button attributes */

fun AttrsScope<HTMLButtonElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsScope<HTMLButtonElement>.disabled() =
    attr("disabled", "")

fun AttrsScope<HTMLButtonElement>.form(formId: String) =
    attr("form", formId)

fun AttrsScope<HTMLButtonElement>.formAction(url: String) =
    attr("formaction", url)

fun AttrsScope<HTMLButtonElement>.formEncType(value: ButtonFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsScope<HTMLButtonElement>.formMethod(value: ButtonFormMethod) =
    attr("formmethod", value.methodStr)

fun AttrsScope<HTMLButtonElement>.formNoValidate() =
    attr("formnovalidate", "")

fun AttrsScope<HTMLButtonElement>.formTarget(value: ButtonFormTarget) =
    attr("formtarget", value.targetStr)

fun AttrsScope<HTMLButtonElement>.name(value: String) =
    attr("name", value)

fun AttrsScope<HTMLButtonElement>.type(value: ButtonType) =
    attr("type", value.str)

fun AttrsScope<HTMLButtonElement>.value(value: String) =
    attr("value", value)

/* Form attributes */

fun AttrsScope<HTMLFormElement>.action(value: String) =
    attr("action", value)

fun AttrsScope<HTMLFormElement>.acceptCharset(value: String) =
    attr("accept-charset", value)

fun AttrsScope<HTMLFormElement>.autoComplete(value: Boolean = true) =
    attr("autocomplete", if(value) "on" else "off")

fun AttrsScope<HTMLFormElement>.encType(value: FormEncType) =
    attr("enctype", value.typeStr)

fun AttrsScope<HTMLFormElement>.method(value: FormMethod) =
    attr("method", value.methodStr)

fun AttrsScope<HTMLFormElement>.noValidate() =
    attr("novalidate", "")

fun AttrsScope<HTMLFormElement>.target(value: FormTarget) =
    attr("target", value.targetStr)

fun AttrsScope<HTMLFormElement>.onSubmit(
    listener: (SyntheticSubmitEvent) -> Unit
) {
    addEventListener(eventName = EventsListenerScope.SUBMIT, listener = listener)
}

fun AttrsScope<HTMLFormElement>.onReset(
    listener: (SyntheticSubmitEvent) -> Unit
) {
    addEventListener(eventName = EventsListenerScope.RESET, listener = listener)
}

/* Input attributes */

fun AttrsScope<HTMLInputElement>.type(value: InputType<*>) =
    attr("type", value.typeStr)

fun AttrsScope<HTMLInputElement>.accept(value: String) =
    attr("accept", value) // type: file only

fun AttrsScope<HTMLInputElement>.alt(value: String) =
    attr("alt", value) // type: image only

fun AttrsScope<HTMLInputElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())

fun AttrsScope<HTMLInputElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsScope<HTMLInputElement>.capture(value: String) =
    attr("capture", value) // type: file only

fun AttrsScope<HTMLInputElement>.dirName(value: String) =
    attr("dirname", value) // text, search

fun AttrsScope<HTMLInputElement>.disabled() =
    attr("disabled", "")

fun AttrsScope<HTMLInputElement>.form(id: String) =
    attr("form", id)

fun AttrsScope<HTMLInputElement>.formAction(url: String) =
    attr("formaction", url)

fun AttrsScope<HTMLInputElement>.formEncType(value: InputFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsScope<HTMLInputElement>.formMethod(value: InputFormMethod) =
    attr("formmethod", value.methodStr)

fun AttrsScope<HTMLInputElement>.formNoValidate() =
    attr("formnovalidate", "")

fun AttrsScope<HTMLInputElement>.formTarget(value: InputFormTarget) =
    attr("formtarget", value.targetStr)

fun AttrsScope<HTMLInputElement>.height(value: Int) =
    attr("height", value.toString()) // image only

fun AttrsScope<HTMLInputElement>.width(value: Int) =
    attr("width", value.toString()) // image only

fun AttrsScope<HTMLCanvasElement>.width(value: Int) =
    attr("width", value.toString())

fun AttrsScope<HTMLCanvasElement>.height(value: Int) =
    attr("height", value.toString())

fun AttrsScope<HTMLInputElement>.list(dataListId: String) =
    attr("list", dataListId)

fun AttrsScope<HTMLInputElement>.max(value: String) =
    attr("max", value)

fun AttrsScope<HTMLInputElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

fun AttrsScope<HTMLInputElement>.min(value: String) =
    attr("min", value)

fun AttrsScope<HTMLInputElement>.minLength(value: Int) =
    attr("minlength", value.toString())

fun AttrsScope<HTMLInputElement>.multiple() =
    attr("multiple", "")

fun AttrsScope<HTMLInputElement>.name(value: String) =
    attr("name", value)

fun AttrsScope<HTMLInputElement>.pattern(value: String) =
    attr("pattern", value)

fun AttrsScope<HTMLInputElement>.placeholder(value: String) =
    attr("placeholder", value)

fun AttrsScope<HTMLInputElement>.readOnly() =
    attr("readonly", "")

@Deprecated(
    message = "Please use `required()` without parameters. Use if..else.. if conditional behaviour required.",
    replaceWith = ReplaceWith("required()", "org.jetbrains.compose.web.attributes.required"),
    level = DeprecationLevel.WARNING
)
fun AttrsScope<HTMLInputElement>.required(value: Boolean = true) =
    attr("required", value.toString())

fun AttrsScope<HTMLInputElement>.required() =
    attr("required", "")

fun AttrsScope<HTMLInputElement>.size(value: Int) =
    attr("size", value.toString())

fun AttrsScope<HTMLInputElement>.src(value: String) =
    attr("src", value) // image only

fun AttrsScope<HTMLInputElement>.step(value: Number) =
    attr("step", value.toString()) // numeric types only

/* Option attributes */

fun AttrsScope<HTMLOptionElement>.value(value: String) =
    attr("value", value)

fun AttrsScope<HTMLOptionElement>.disabled() =
    attr("disabled", "")

fun AttrsScope<HTMLOptionElement>.selected() =
    attr("selected", "")

fun AttrsScope<HTMLOptionElement>.label(value: String) =
    attr("label", value)

/* Select attributes */

fun AttrsScope<HTMLSelectElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())

fun AttrsScope<HTMLSelectElement>.autofocus() =
    attr("autofocus", "")

fun AttrsScope<HTMLSelectElement>.disabled() =
    attr("disabled", "")

fun AttrsScope<HTMLSelectElement>.form(formId: String) =
    attr("form", formId)

fun AttrsScope<HTMLSelectElement>.multiple() =
    attr("multiple", "")

fun AttrsScope<HTMLSelectElement>.name(value: String) =
    attr("name", value)

fun AttrsScope<HTMLSelectElement>.required() =
    attr("required", "")

fun AttrsScope<HTMLSelectElement>.size(numberOfRows: Int) =
    attr("size", numberOfRows.toString())

/* OptGroup attributes */

fun AttrsScope<HTMLOptGroupElement>.label(value: String) =
    attr("label", value)

fun AttrsScope<HTMLOptGroupElement>.disabled() =
    attr("disabled", "")

/* TextArea attributes */

fun AttrsScope<HTMLTextAreaElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())

fun AttrsScope<HTMLTextAreaElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsScope<HTMLTextAreaElement>.cols(value: Int) =
    attr("cols", value.toString())

fun AttrsScope<HTMLTextAreaElement>.disabled() =
    attr("disabled", "")

fun AttrsScope<HTMLTextAreaElement>.form(formId: String) =
    attr("form", formId)

fun AttrsScope<HTMLTextAreaElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

fun AttrsScope<HTMLTextAreaElement>.minLength(value: Int) =
    attr("minlength", value.toString())

fun AttrsScope<HTMLTextAreaElement>.name(value: String) =
    attr("name", value)

fun AttrsScope<HTMLTextAreaElement>.placeholder(value: String) =
    attr("placeholder", value)

fun AttrsScope<HTMLTextAreaElement>.readOnly() =
    attr("readonly", "")

fun AttrsScope<HTMLTextAreaElement>.required() =
    attr("required", "")

fun AttrsScope<HTMLTextAreaElement>.rows(value: Int) =
    attr("rows", value.toString())

fun AttrsScope<HTMLTextAreaElement>.wrap(value: TextAreaWrap) =
    attr("wrap", value.str)

/* Img attributes */

fun AttrsScope<HTMLImageElement>.src(value: String): AttrsScope<HTMLImageElement> =
    attr("src", value)

fun AttrsScope<HTMLImageElement>.alt(value: String): AttrsScope<HTMLImageElement> =
    attr("alt", value)


internal val setInputValue: (HTMLInputElement, String) -> Unit = { e, v ->
    if (v != e.value) {
        e.value = v
    }
    saveControlledInputState(e, v)
}

internal val setTextAreaDefaultValue: (HTMLTextAreaElement, String) -> Unit = { e, v ->
    e.innerText = v
}

internal val setCheckedValue: (HTMLInputElement, Boolean) -> Unit = { e, v ->
    e.checked = v
    saveControlledInputState(e, v)
}

/* Img attributes */
fun AttrsScope<HTMLLabelElement>.forId(value: String): AttrsScope<HTMLLabelElement> =
    attr("for", value)

/* Table attributes */
fun AttrsScope<HTMLTableColElement>.span(value: Int): AttrsScope<HTMLTableColElement> =
    attr("span", value.toString())

fun AttrsScope<HTMLTableCellElement>.scope(value: Scope): AttrsScope<HTMLTableCellElement> =
    attr("scope", value.str)

fun AttrsScope<HTMLTableCellElement>.colspan(value: Int): AttrsScope<HTMLTableCellElement> =
    attr("colspan", value.toString())

fun AttrsScope<HTMLTableCellElement>.rowspan(value: Int): AttrsScope<HTMLTableCellElement> =
    attr("rowspan", value.toString())
