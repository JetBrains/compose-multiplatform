/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.web.attributes

import org.w3c.dom.HTMLInputElement

open class Tag {
    object Div : Tag()
    object A : Tag()
    object Button : Tag()
    object Form : Tag()
    object Input : Tag()
    object Select : Tag()
    object Option : Tag()
    object OptGroup : Tag()
    object H : Tag()
    object Ul : Tag()
    object Ol : Tag()
    object Li : Tag()
    object Img : Tag()
    object TextArea : Tag()
    object Nav : Tag()
    object Span : Tag()
    object P : Tag()
    object Br : Tag()
    object Style : Tag()
    object Pre : Tag()
    object Code : Tag()
    object Label : Tag()
    object Table : Tag()
    object Caption : Tag()
    object Col : Tag()
    object Colgroup : Tag()
    object Tr : Tag()
    object Thead : Tag()
    object Th : Tag()
    object Td : Tag()
    object Tbody : Tag()
    object Tfoot : Tag()
}

/* Anchor <a> attributes */

fun AttrsBuilder<Tag.A>.href(value: String?) =
    attr("href", value)

fun AttrsBuilder<Tag.A>.target(value: ATarget = ATarget.Self) =
    attr("target", value.targetStr)

fun AttrsBuilder<Tag.A>.ref(value: ARel) =
    attr("rel", value.relStr)

fun AttrsBuilder<Tag.A>.ping(value: String) =
    attr("ping", value)

fun AttrsBuilder<Tag.A>.ping(vararg urls: String) =
    attr("ping", urls.joinToString(" "))

fun AttrsBuilder<Tag.A>.hreflang(value: String) =
    attr("hreflang", value)

fun AttrsBuilder<Tag.A>.download(value: String = "") =
    attr("download", value)

/* Button attributes */

fun AttrsBuilder<Tag.Button>.autoFocus(value: Boolean = true) =
    attr("autofocus", if (value) "" else null)

fun AttrsBuilder<Tag.Button>.disabled(value: Boolean = true) =
    attr("disabled", if (value) "" else null)

fun AttrsBuilder<Tag.Button>.form(formId: String) =
    attr("form", formId)

fun AttrsBuilder<Tag.Button>.formAction(url: String) =
    attr("formaction", url)

fun AttrsBuilder<Tag.Button>.formEncType(value: ButtonFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsBuilder<Tag.Button>.formMethod(value: ButtonFormMethod) =
    attr("formmethod", value.methodStr)

fun AttrsBuilder<Tag.Button>.formNoValidate(value: Boolean = true) =
    attr("formnovalidate", if (value) "" else null)

fun AttrsBuilder<Tag.Button>.formTarget(value: ButtonFormTarget) =
    attr("formtarget", value.targetStr)

fun AttrsBuilder<Tag.Button>.name(value: String) =
    attr("name", value)

fun AttrsBuilder<Tag.Button>.type(value: ButtonType) =
    attr("type", value.str)

fun AttrsBuilder<Tag.Button>.value(value: String) =
    attr("value", value)

/* Form attributes */

fun AttrsBuilder<Tag.Form>.action(value: String) =
    attr("action", value)

fun AttrsBuilder<Tag.Form>.acceptCharset(value: String) =
    attr("accept-charset", value)

fun AttrsBuilder<Tag.Form>.autoComplete(value: Boolean) =
    attr("autocomplete", if (value) "" else null)

fun AttrsBuilder<Tag.Form>.encType(value: FormEncType) =
    attr("enctype", value.typeStr)

fun AttrsBuilder<Tag.Form>.method(value: FormMethod) =
    attr("method", value.methodStr)

fun AttrsBuilder<Tag.Form>.noValidate(value: Boolean = true) =
    attr("novalidate", if (value) "" else null)

fun AttrsBuilder<Tag.Form>.target(value: FormTarget) =
    attr("target", value.targetStr)

/* Input attributes */

fun AttrsBuilder<Tag.Input>.type(value: InputType) =
    attr("type", value.typeStr)

fun AttrsBuilder<Tag.Input>.accept(value: String) =
    attr("accept", value) // type: file only

fun AttrsBuilder<Tag.Input>.alt(value: String) =
    attr("alt", value) // type: image only

fun AttrsBuilder<Tag.Input>.autoComplete(value: Boolean = true) =
    attr("autocomplete", if (value) "" else null)

fun AttrsBuilder<Tag.Input>.autoFocus(value: Boolean = true) =
    attr("autofocus", if (value) "" else null)

fun AttrsBuilder<Tag.Input>.capture(value: String) =
    attr("capture", value) // type: file only

fun AttrsBuilder<Tag.Input>.checked(value: Boolean = true) =
    attr("checked", if (value) "" else null) // radio, checkbox

fun AttrsBuilder<Tag.Input>.dirName(value: String) =
    attr("dirname", value) // text, search

fun AttrsBuilder<Tag.Input>.disabled(value: Boolean = true) =
    attr("disabled", if (value) "" else null)

fun AttrsBuilder<Tag.Input>.form(id: String) =
    attr("form", id)

fun AttrsBuilder<Tag.Input>.formAction(url: String) =
    attr("formaction", url)

fun AttrsBuilder<Tag.Input>.formEncType(value: InputFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsBuilder<Tag.Input>.formMethod(value: InputFormMethod) =
    attr("formmethod", value.methodStr)

fun AttrsBuilder<Tag.Input>.formNoValidate(value: Boolean = true) =
    attr("formnovalidate", if (value) "" else null)

fun AttrsBuilder<Tag.Input>.formTarget(value: InputFormTarget) =
    attr("formtarget", value.targetStr)

fun AttrsBuilder<Tag.Input>.height(value: Int) =
    attr("height", value.toString()) // image only

fun AttrsBuilder<Tag.Input>.width(value: Int) =
    attr("width", value.toString()) // image only

fun AttrsBuilder<Tag.Input>.list(dataListId: String) =
    attr("list", dataListId)

fun AttrsBuilder<Tag.Input>.max(value: String) =
    attr("max", value)

fun AttrsBuilder<Tag.Input>.maxLength(value: Int) =
    attr("maxlength", value.toString())

fun AttrsBuilder<Tag.Input>.min(value: String) =
    attr("min", value)

fun AttrsBuilder<Tag.Input>.minLength(value: Int) =
    attr("minlength", value.toString())

fun AttrsBuilder<Tag.Input>.multiple(value: Boolean = true) =
    attr("multiple", if (value) "" else null)

fun AttrsBuilder<Tag.Input>.name(value: String) =
    attr("name", value)

fun AttrsBuilder<Tag.Input>.pattern(value: String) =
    attr("pattern", value)

fun AttrsBuilder<Tag.Input>.placeholder(value: String) =
    attr("placeholder", value)

fun AttrsBuilder<Tag.Input>.readOnly(value: Boolean = true) =
    attr("readonly", if (value) "" else null)

fun AttrsBuilder<Tag.Input>.required(value: Boolean = true) =
    attr("required", value.toString())

fun AttrsBuilder<Tag.Input>.size(value: Int) =
    attr("size", value.toString())

fun AttrsBuilder<Tag.Input>.src(value: String) =
    attr("src", value.toString()) // image only

fun AttrsBuilder<Tag.Input>.step(value: Int) =
    attr("step", value.toString()) // numeric types only

fun AttrsBuilder<Tag.Input>.valueAttr(value: String) =
    attr("value", value)

fun AttrsBuilder<Tag.Input>.value(value: String): AttrsBuilder<Tag.Input> {
    prop(setInputValue, value)
    return this
}

/* Option attributes */

fun AttrsBuilder<Tag.Option>.value(value: String) =
    attr("value", value)

fun AttrsBuilder<Tag.Option>.disabled(value: Boolean = true) =
    attr("disabled", if (value) "" else null)

fun AttrsBuilder<Tag.Option>.selected(value: Boolean = true) =
    attr("selected", if (value) "" else null)

fun AttrsBuilder<Tag.Option>.label(value: String) =
    attr("label", value)

/* Select attributes */

fun AttrsBuilder<Tag.Select>.autocomplete(value: String) =
    attr("autocomplete", value)

fun AttrsBuilder<Tag.Select>.autofocus(value: Boolean = true) =
    attr("autofocus", if (value) "" else null)

fun AttrsBuilder<Tag.Select>.disabled(value: Boolean = true) =
    attr("disabled", if (value) "" else null)

fun AttrsBuilder<Tag.Select>.form(formId: String) =
    attr("form", formId)

fun AttrsBuilder<Tag.Select>.multiple(value: Boolean = true) =
    attr("multiple", if (value) "" else null)

fun AttrsBuilder<Tag.Select>.name(value: String) =
    attr("name", value)

fun AttrsBuilder<Tag.Select>.required(value: Boolean = true) =
    attr("required", if (value) "" else null)

fun AttrsBuilder<Tag.Select>.size(numberOfRows: Int) =
    attr("size", numberOfRows.toString())

/* OptGroup attributes */

fun AttrsBuilder<Tag.OptGroup>.label(value: String) =
    attr("label", value)

fun AttrsBuilder<Tag.OptGroup>.disabled(value: Boolean = true) =
    attr("disabled", if (value) "" else null)

/* TextArea attributes */

fun AttrsBuilder<Tag.TextArea>.autoComplete(value: Boolean = true) =
    attr("autocomplete", if (value) "on" else "off")

fun AttrsBuilder<Tag.TextArea>.autoFocus(value: Boolean = true) =
    attr("autofocus", if (value) "" else null)

fun AttrsBuilder<Tag.TextArea>.cols(value: Int) =
    attr("cols", value.toString())

fun AttrsBuilder<Tag.TextArea>.disabled(value: Boolean = true) =
    attr("disabled", if (value) "" else null)

fun AttrsBuilder<Tag.TextArea>.form(formId: String) =
    attr("form", formId)

fun AttrsBuilder<Tag.TextArea>.maxLength(value: Int) =
    attr("maxlength", value.toString())

fun AttrsBuilder<Tag.TextArea>.minLength(value: Int) =
    attr("minlength", value.toString())

fun AttrsBuilder<Tag.TextArea>.name(value: String) =
    attr("name", value)

fun AttrsBuilder<Tag.TextArea>.placeholder(value: String) =
    attr("placeholder", value)

fun AttrsBuilder<Tag.TextArea>.readOnly(value: Boolean = true) =
    attr("readonly", if (value) "" else null)

fun AttrsBuilder<Tag.TextArea>.required(value: Boolean = true) =
    attr("required", if (value) "" else null)

fun AttrsBuilder<Tag.TextArea>.rows(value: Int) =
    attr("rows", value.toString())

fun AttrsBuilder<Tag.TextArea>.wrap(value: TextAreaWrap) =
    attr("wrap", value.str)

fun AttrsBuilder<Tag.TextArea>.value(value: String): AttrsBuilder<Tag.TextArea> {
    prop(setInputValue, value)
    return this
}

/* Img attributes */

fun AttrsBuilder<Tag.Img>.src(value: String?): AttrsBuilder<Tag.Img> =
    attr("src", value)

fun AttrsBuilder<Tag.Img>.alt(value: String?): AttrsBuilder<Tag.Img> =
    attr("alt", value)

private val setInputValue: (HTMLInputElement, String) -> Unit = { e, v ->
    e.value = v
}

/* Img attributes */
fun AttrsBuilder<Tag.Label>.forId(value: String?): AttrsBuilder<Tag.Label> =
    attr("for", value)

/* Table attributes */
fun AttrsBuilder<Tag.Th>.scope(value: Scope?): AttrsBuilder<Tag.Th> =
    attr("scope", value?.str)

fun AttrsBuilder<Tag.Col>.span(value: Int): AttrsBuilder<Tag.Col> =
    attr("span", value.toString())

fun AttrsBuilder<Tag.Th>.colspan(value: Int): AttrsBuilder<Tag.Th> =
    attr("colspan", value.toString())

fun AttrsBuilder<Tag.Th>.rowspan(value: Int): AttrsBuilder<Tag.Th> =
    attr("rowspan", value.toString())

fun AttrsBuilder<Tag.Td>.colspan(value: Int): AttrsBuilder<Tag.Td> =
    attr("colspan", value.toString())

fun AttrsBuilder<Tag.Td>.rowspan(value: Int): AttrsBuilder<Tag.Td> =
    attr("rowspan", value.toString())
