package org.jetbrains.compose.web.attributes

import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
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

fun AttrsBuilder<HTMLAnchorElement>.href(value: String) =
    attr("href", value)

fun AttrsBuilder<HTMLAnchorElement>.target(value: ATarget = ATarget.Self) =
    attr("target", value.targetStr)

fun AttrsBuilder<HTMLAnchorElement>.ref(value: ARel) =
    attr("rel", value.relStr)

fun AttrsBuilder<HTMLAnchorElement>.ping(value: String) =
    attr("ping", value)

fun AttrsBuilder<HTMLAnchorElement>.ping(vararg urls: String) =
    attr("ping", urls.joinToString(" "))

fun AttrsBuilder<HTMLAnchorElement>.hreflang(value: String) =
    attr("hreflang", value)

fun AttrsBuilder<HTMLAnchorElement>.download(value: String = "") =
    attr("download", value)

/* Button attributes */

fun AttrsBuilder<HTMLButtonElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsBuilder<HTMLButtonElement>.disabled() =
    attr("disabled", "")

fun AttrsBuilder<HTMLButtonElement>.form(formId: String) =
    attr("form", formId)

fun AttrsBuilder<HTMLButtonElement>.formAction(url: String) =
    attr("formaction", url)

fun AttrsBuilder<HTMLButtonElement>.formEncType(value: ButtonFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsBuilder<HTMLButtonElement>.formMethod(value: ButtonFormMethod) =
    attr("formmethod", value.methodStr)

fun AttrsBuilder<HTMLButtonElement>.formNoValidate() =
    attr("formnovalidate", "")

fun AttrsBuilder<HTMLButtonElement>.formTarget(value: ButtonFormTarget) =
    attr("formtarget", value.targetStr)

fun AttrsBuilder<HTMLButtonElement>.name(value: String) =
    attr("name", value)

fun AttrsBuilder<HTMLButtonElement>.type(value: ButtonType) =
    attr("type", value.str)

fun AttrsBuilder<HTMLButtonElement>.value(value: String) =
    attr("value", value)

/* Form attributes */

fun AttrsBuilder<HTMLFormElement>.action(value: String) =
    attr("action", value)

fun AttrsBuilder<HTMLFormElement>.acceptCharset(value: String) =
    attr("accept-charset", value)

fun AttrsBuilder<HTMLFormElement>.autoComplete(value: Boolean = true) =
    attr("autocomplete", if(value) "on" else "off")

fun AttrsBuilder<HTMLFormElement>.encType(value: FormEncType) =
    attr("enctype", value.typeStr)

fun AttrsBuilder<HTMLFormElement>.method(value: FormMethod) =
    attr("method", value.methodStr)

fun AttrsBuilder<HTMLFormElement>.noValidate() =
    attr("novalidate", "")

fun AttrsBuilder<HTMLFormElement>.target(value: FormTarget) =
    attr("target", value.targetStr)

/* Input attributes */

fun AttrsBuilder<HTMLInputElement>.type(value: InputType<*>) =
    attr("type", value.typeStr)

fun AttrsBuilder<HTMLInputElement>.accept(value: String) =
    attr("accept", value) // type: file only

fun AttrsBuilder<HTMLInputElement>.alt(value: String) =
    attr("alt", value) // type: image only

fun AttrsBuilder<HTMLInputElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())

fun AttrsBuilder<HTMLInputElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsBuilder<HTMLInputElement>.capture(value: String) =
    attr("capture", value) // type: file only

fun AttrsBuilder<HTMLInputElement>.checked() =
    attr("checked", "") // radio, checkbox

fun AttrsBuilder<HTMLInputElement>.dirName(value: String) =
    attr("dirname", value) // text, search

fun AttrsBuilder<HTMLInputElement>.disabled() =
    attr("disabled", "")

fun AttrsBuilder<HTMLInputElement>.form(id: String) =
    attr("form", id)

fun AttrsBuilder<HTMLInputElement>.formAction(url: String) =
    attr("formaction", url)

fun AttrsBuilder<HTMLInputElement>.formEncType(value: InputFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsBuilder<HTMLInputElement>.formMethod(value: InputFormMethod) =
    attr("formmethod", value.methodStr)

fun AttrsBuilder<HTMLInputElement>.formNoValidate() =
    attr("formnovalidate", "")

fun AttrsBuilder<HTMLInputElement>.formTarget(value: InputFormTarget) =
    attr("formtarget", value.targetStr)

fun AttrsBuilder<HTMLInputElement>.height(value: Int) =
    attr("height", value.toString()) // image only

fun AttrsBuilder<HTMLInputElement>.width(value: Int) =
    attr("width", value.toString()) // image only

fun AttrsBuilder<HTMLInputElement>.list(dataListId: String) =
    attr("list", dataListId)

fun AttrsBuilder<HTMLInputElement>.max(value: String) =
    attr("max", value)

fun AttrsBuilder<HTMLInputElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

fun AttrsBuilder<HTMLInputElement>.min(value: String) =
    attr("min", value)

fun AttrsBuilder<HTMLInputElement>.minLength(value: Int) =
    attr("minlength", value.toString())

fun AttrsBuilder<HTMLInputElement>.multiple() =
    attr("multiple", "")

fun AttrsBuilder<HTMLInputElement>.name(value: String) =
    attr("name", value)

fun AttrsBuilder<HTMLInputElement>.pattern(value: String) =
    attr("pattern", value)

fun AttrsBuilder<HTMLInputElement>.placeholder(value: String) =
    attr("placeholder", value)

fun AttrsBuilder<HTMLInputElement>.readOnly() =
    attr("readonly", "")

fun AttrsBuilder<HTMLInputElement>.required(value: Boolean = true) =
    attr("required", value.toString())

fun AttrsBuilder<HTMLInputElement>.size(value: Int) =
    attr("size", value.toString())

fun AttrsBuilder<HTMLInputElement>.src(value: String) =
    attr("src", value) // image only

fun AttrsBuilder<HTMLInputElement>.step(value: Number) =
    attr("step", value.toString()) // numeric types only

fun AttrsBuilder<HTMLInputElement>.valueAttr(value: String) =
    attr("value", value)

fun AttrsBuilder<HTMLInputElement>.value(value: String): AttrsBuilder<HTMLInputElement> {
    prop(setInputValue, value)
    return this
}

/* Option attributes */

fun AttrsBuilder<HTMLOptionElement>.value(value: String) =
    attr("value", value)

fun AttrsBuilder<HTMLOptionElement>.disabled() =
    attr("disabled", "")

fun AttrsBuilder<HTMLOptionElement>.selected() =
    attr("selected", "")

fun AttrsBuilder<HTMLOptionElement>.label(value: String) =
    attr("label", value)

/* Select attributes */

fun AttrsBuilder<HTMLSelectElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())

fun AttrsBuilder<HTMLSelectElement>.autofocus() =
    attr("autofocus", "")

fun AttrsBuilder<HTMLSelectElement>.disabled() =
    attr("disabled", "")

fun AttrsBuilder<HTMLSelectElement>.form(formId: String) =
    attr("form", formId)

fun AttrsBuilder<HTMLSelectElement>.multiple() =
    attr("multiple", "")

fun AttrsBuilder<HTMLSelectElement>.name(value: String) =
    attr("name", value)

fun AttrsBuilder<HTMLSelectElement>.required() =
    attr("required", "")

fun AttrsBuilder<HTMLSelectElement>.size(numberOfRows: Int) =
    attr("size", numberOfRows.toString())

/* OptGroup attributes */

fun AttrsBuilder<HTMLOptGroupElement>.label(value: String) =
    attr("label", value)

fun AttrsBuilder<HTMLOptGroupElement>.disabled() =
    attr("disabled", "")

/* TextArea attributes */

fun AttrsBuilder<HTMLTextAreaElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())

fun AttrsBuilder<HTMLTextAreaElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsBuilder<HTMLTextAreaElement>.cols(value: Int) =
    attr("cols", value.toString())

fun AttrsBuilder<HTMLTextAreaElement>.disabled() =
    attr("disabled", "")

fun AttrsBuilder<HTMLTextAreaElement>.form(formId: String) =
    attr("form", formId)

fun AttrsBuilder<HTMLTextAreaElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

fun AttrsBuilder<HTMLTextAreaElement>.minLength(value: Int) =
    attr("minlength", value.toString())

fun AttrsBuilder<HTMLTextAreaElement>.name(value: String) =
    attr("name", value)

fun AttrsBuilder<HTMLTextAreaElement>.placeholder(value: String) =
    attr("placeholder", value)

fun AttrsBuilder<HTMLTextAreaElement>.readOnly() =
    attr("readonly", "")

fun AttrsBuilder<HTMLTextAreaElement>.required() =
    attr("required", "")

fun AttrsBuilder<HTMLTextAreaElement>.rows(value: Int) =
    attr("rows", value.toString())

fun AttrsBuilder<HTMLTextAreaElement>.wrap(value: TextAreaWrap) =
    attr("wrap", value.str)

fun AttrsBuilder<HTMLTextAreaElement>.value(value: String): AttrsBuilder<HTMLTextAreaElement> {
    prop(setInputValue, value)
    return this
}

/* Img attributes */

fun AttrsBuilder<HTMLImageElement>.src(value: String): AttrsBuilder<HTMLImageElement> =
    attr("src", value)

fun AttrsBuilder<HTMLImageElement>.alt(value: String): AttrsBuilder<HTMLImageElement> =
    attr("alt", value)

private val setInputValue: (HTMLInputElement, String) -> Unit = { e, v ->
    e.value = v
}

/* Img attributes */
fun AttrsBuilder<HTMLLabelElement>.forId(value: String): AttrsBuilder<HTMLLabelElement> =
    attr("for", value)

/* Table attributes */
fun AttrsBuilder<HTMLTableColElement>.span(value: Int): AttrsBuilder<HTMLTableColElement> =
    attr("span", value.toString())

fun AttrsBuilder<HTMLTableCellElement>.scope(value: Scope): AttrsBuilder<HTMLTableCellElement> =
    attr("scope", value.str)

fun AttrsBuilder<HTMLTableCellElement>.colspan(value: Int): AttrsBuilder<HTMLTableCellElement> =
    attr("colspan", value.toString())

fun AttrsBuilder<HTMLTableCellElement>.rowspan(value: Int): AttrsBuilder<HTMLTableCellElement> =
    attr("rowspan", value.toString())

