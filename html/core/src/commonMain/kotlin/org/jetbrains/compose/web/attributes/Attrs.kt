package org.jetbrains.compose.web.attributes

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
import kotlin.jvm.JvmName

fun AttrsScope<HTMLAnchorElement>.href(value: String) =
    attr("href", value)

fun AttrsScope<HTMLAnchorElement>.target(value: ATarget = ATarget.Self) =
    attr("target", value.targetStr)

fun AttrsScope<HTMLAnchorElement>.rel(value: ARel) =
    attr("rel", value.relStr)

@Deprecated(
    message = "Use `rel` instead.",
    replaceWith = ReplaceWith("rel(value)", "org.jetbrains.compose.web.attributes.rel"),
    level = DeprecationLevel.WARNING,
)
fun AttrsScope<HTMLAnchorElement>.ref(value: ARel) =
    rel(value)

fun AttrsScope<HTMLAnchorElement>.ping(value: String) =
    attr("ping", value)

fun AttrsScope<HTMLAnchorElement>.ping(vararg urls: String) =
    attr("ping", urls.joinToString(" "))

fun AttrsScope<HTMLAnchorElement>.hreflang(value: String) =
    attr("hreflang", value)

fun AttrsScope<HTMLAnchorElement>.download(value: String = "") =
    attr("download", value)

/* Link attributes */

@JvmName("linkHref")
fun AttrsScope<HTMLLinkElement>.href(value: String) =
    attr("href", value)

fun AttrsScope<HTMLLinkElement>.rel(
    value: LinkRel,
    vararg additionalValues: LinkRel,
) = attr(
    "rel",
    listOf(value, *additionalValues).joinToString(" ") { it.relStr },
)

fun AttrsScope<HTMLLinkElement>.type(value: String) =
    attr("type", value)

/* Script attributes */

@JvmName("scriptSrc")
fun AttrsScope<HTMLScriptElement>.src(value: String) =
    attr("src", value)

@JvmName("scriptType")
fun AttrsScope<HTMLScriptElement>.type(value: ScriptType) =
    attr("type", value.typeStr)

/* Button attributes */

@JvmName("buttonAutoFocus")
fun AttrsScope<HTMLButtonElement>.autoFocus() =
    attr("autofocus", "")

@JvmName("buttonDisabled")
fun AttrsScope<HTMLButtonElement>.disabled() =
    attr("disabled", "")

@JvmName("buttonForm")
fun AttrsScope<HTMLButtonElement>.form(formId: String) =
    attr("form", formId)

@JvmName("buttonFormAction")
fun AttrsScope<HTMLButtonElement>.formAction(url: String) =
    attr("formaction", url)

fun AttrsScope<HTMLButtonElement>.formEncType(value: ButtonFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsScope<HTMLButtonElement>.formMethod(value: ButtonFormMethod) =
    attr("formmethod", value.methodStr)

@JvmName("buttonFormNoValidate")
fun AttrsScope<HTMLButtonElement>.formNoValidate() =
    attr("formnovalidate", "")

fun AttrsScope<HTMLButtonElement>.formTarget(value: ButtonFormTarget) =
    attr("formtarget", value.targetStr)

@JvmName("buttonName")
fun AttrsScope<HTMLButtonElement>.name(value: String) =
    attr("name", value)

fun AttrsScope<HTMLButtonElement>.type(value: ButtonType) =
    attr("type", value.str)

@JvmName("buttonValue")
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

// JVM erases AttrsScope's element type, so element-specific extensions with otherwise identical signatures need distinct JVM names.
// Web targets do not use JVM bytecode signatures, so they have no equivalent signature clash.
@JvmName("inputAlt")
fun AttrsScope<HTMLInputElement>.alt(value: String) =
    attr("alt", value) // type: image only

@JvmName("inputAutoComplete")
fun AttrsScope<HTMLInputElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.toString())

@JvmName("inputAutoFocus")
fun AttrsScope<HTMLInputElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsScope<HTMLInputElement>.capture(value: String) =
    attr("capture", value) // type: file only

fun AttrsScope<HTMLInputElement>.dirName(value: String) =
    attr("dirname", value) // text, search

@JvmName("inputDisabled")
fun AttrsScope<HTMLInputElement>.disabled() =
    attr("disabled", "")

@JvmName("inputForm")
fun AttrsScope<HTMLInputElement>.form(id: String) =
    attr("form", id)

@JvmName("inputFormAction")
fun AttrsScope<HTMLInputElement>.formAction(url: String) =
    attr("formaction", url)

fun AttrsScope<HTMLInputElement>.formEncType(value: InputFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsScope<HTMLInputElement>.formMethod(value: InputFormMethod) =
    attr("formmethod", value.methodStr)

@JvmName("inputFormNoValidate")
fun AttrsScope<HTMLInputElement>.formNoValidate() =
    attr("formnovalidate", "")

fun AttrsScope<HTMLInputElement>.formTarget(value: InputFormTarget) =
    attr("formtarget", value.targetStr)

@JvmName("inputHeight")
fun AttrsScope<HTMLInputElement>.height(value: Int) =
    attr("height", value.toString()) // image only

@JvmName("inputWidth")
fun AttrsScope<HTMLInputElement>.width(value: Int) =
    attr("width", value.toString()) // image only

@JvmName("canvasWidth")
fun AttrsScope<HTMLCanvasElement>.width(value: Int) =
    attr("width", value.toString())

@JvmName("canvasHeight")
fun AttrsScope<HTMLCanvasElement>.height(value: Int) =
    attr("height", value.toString())

fun AttrsScope<HTMLInputElement>.list(dataListId: String) =
    attr("list", dataListId)

fun AttrsScope<HTMLInputElement>.max(value: String) =
    attr("max", value)

@JvmName("inputMaxLength")
fun AttrsScope<HTMLInputElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

fun AttrsScope<HTMLInputElement>.min(value: String) =
    attr("min", value)

@JvmName("inputMinLength")
fun AttrsScope<HTMLInputElement>.minLength(value: Int) =
    attr("minlength", value.toString())

@JvmName("inputMultiple")
fun AttrsScope<HTMLInputElement>.multiple() =
    attr("multiple", "")

@JvmName("inputName")
fun AttrsScope<HTMLInputElement>.name(value: String) =
    attr("name", value)

fun AttrsScope<HTMLInputElement>.pattern(value: String) =
    attr("pattern", value)

@JvmName("inputPlaceholder")
fun AttrsScope<HTMLInputElement>.placeholder(value: String) =
    attr("placeholder", value)

@JvmName("inputReadOnly")
fun AttrsScope<HTMLInputElement>.readOnly() =
    attr("readonly", "")

@Deprecated(
    message = "Please use `required()` without parameters. Use if..else.. if conditional behaviour required.",
    replaceWith = ReplaceWith("required()", "org.jetbrains.compose.web.attributes.required"),
    level = DeprecationLevel.WARNING
)
@JvmName("inputRequired")
fun AttrsScope<HTMLInputElement>.required(value: Boolean = true) =
    attr("required", value.toString())

@JvmName("inputRequired")
fun AttrsScope<HTMLInputElement>.required() =
    attr("required", "")

@JvmName("inputSize")
fun AttrsScope<HTMLInputElement>.size(value: Int) =
    attr("size", value.toString())

@JvmName("inputSrc")
fun AttrsScope<HTMLInputElement>.src(value: String) =
    attr("src", value) // image only

fun AttrsScope<HTMLInputElement>.step(value: Number) =
    attr("step", value.toString()) // numeric types only

/* Option attributes */

@JvmName("optionValue")
fun AttrsScope<HTMLOptionElement>.value(value: String) =
    attr("value", value)

@JvmName("optionDisabled")
fun AttrsScope<HTMLOptionElement>.disabled() =
    attr("disabled", "")

fun AttrsScope<HTMLOptionElement>.selected() =
    attr("selected", "")

@JvmName("optionLabel")
fun AttrsScope<HTMLOptionElement>.label(value: String) =
    attr("label", value)

/* Select attributes */

@JvmName("selectAutoComplete")
fun AttrsScope<HTMLSelectElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.toString())

fun AttrsScope<HTMLSelectElement>.autofocus() =
    attr("autofocus", "")

@JvmName("selectDisabled")
fun AttrsScope<HTMLSelectElement>.disabled() =
    attr("disabled", "")

@JvmName("selectForm")
fun AttrsScope<HTMLSelectElement>.form(formId: String) =
    attr("form", formId)

@JvmName("selectMultiple")
fun AttrsScope<HTMLSelectElement>.multiple() =
    attr("multiple", "")

@JvmName("selectName")
fun AttrsScope<HTMLSelectElement>.name(value: String) =
    attr("name", value)

@JvmName("selectRequired")
fun AttrsScope<HTMLSelectElement>.required() =
    attr("required", "")

@JvmName("selectSize")
fun AttrsScope<HTMLSelectElement>.size(numberOfRows: Int) =
    attr("size", numberOfRows.toString())

/* OptGroup attributes */

@JvmName("optGroupLabel")
fun AttrsScope<HTMLOptGroupElement>.label(value: String) =
    attr("label", value)

@JvmName("optGroupDisabled")
fun AttrsScope<HTMLOptGroupElement>.disabled() =
    attr("disabled", "")

/* TextArea attributes */

@JvmName("textAreaAutoComplete")
fun AttrsScope<HTMLTextAreaElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.toString())

@JvmName("textAreaAutoFocus")
fun AttrsScope<HTMLTextAreaElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsScope<HTMLTextAreaElement>.cols(value: Int) =
    attr("cols", value.toString())

@JvmName("textAreaDisabled")
fun AttrsScope<HTMLTextAreaElement>.disabled() =
    attr("disabled", "")

@JvmName("textAreaForm")
fun AttrsScope<HTMLTextAreaElement>.form(formId: String) =
    attr("form", formId)

@JvmName("textAreaMaxLength")
fun AttrsScope<HTMLTextAreaElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

@JvmName("textAreaMinLength")
fun AttrsScope<HTMLTextAreaElement>.minLength(value: Int) =
    attr("minlength", value.toString())

@JvmName("textAreaName")
fun AttrsScope<HTMLTextAreaElement>.name(value: String) =
    attr("name", value)

@JvmName("textAreaPlaceholder")
fun AttrsScope<HTMLTextAreaElement>.placeholder(value: String) =
    attr("placeholder", value)

@JvmName("textAreaReadOnly")
fun AttrsScope<HTMLTextAreaElement>.readOnly() =
    attr("readonly", "")

@JvmName("textAreaRequired")
fun AttrsScope<HTMLTextAreaElement>.required() =
    attr("required", "")

fun AttrsScope<HTMLTextAreaElement>.rows(value: Int) =
    attr("rows", value.toString())

fun AttrsScope<HTMLTextAreaElement>.wrap(value: TextAreaWrap) =
    attr("wrap", value.str)

/* Img attributes */

@JvmName("imageSrc")
fun AttrsScope<HTMLImageElement>.src(value: String): AttrsScope<HTMLImageElement> =
    attr("src", value)

@JvmName("imageAlt")
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
