package org.jetbrains.compose.web.attributes

import org.w3c.dom.events.Event

sealed class InputType<T>(val typeStr: String) {

    object Button : InputTypeWithUnitValue("button")
    object Checkbox : InputTypeCheckedValue("checkbox")
    object Color : InputTypeWithStringValue("color")
    object Date : InputTypeWithStringValue("date")
    object DateTimeLocal : InputTypeWithStringValue("datetime-local")
    object Email : InputTypeWithStringValue("email")
    object File : InputTypeWithStringValue("file")
    object Hidden : InputTypeWithStringValue("hidden")
    object Month : InputTypeWithStringValue("month")
    object Number : InputTypeNumberValue("number")
    object Password : InputTypeWithStringValue("password")
    object Radio : InputTypeCheckedValue("radio")
    object Range : InputTypeNumberValue("range")
    object Search : InputTypeWithStringValue("search")
    object Submit : InputTypeWithUnitValue("submit")
    object Tel : InputTypeWithStringValue("tel")
    object Text : InputTypeWithStringValue("text")
    object Time : InputTypeWithStringValue("time")
    object Url : InputTypeWithStringValue("url")
    object Week : InputTypeWithStringValue("week")

    open class InputTypeWithStringValue(name: String) : InputType<String>(name) {
        override fun inputValue(event: Event) = Week.valueAsString(event)
    }

    open class InputTypeWithUnitValue(name: String) : InputType<Unit>(name) {
        override fun inputValue(event: Event) = Unit
    }

    open class InputTypeCheckedValue(name: String) : InputType<Boolean>(name) {
        override fun inputValue(event: Event): Boolean {
            return event.target?.asDynamic()?.checked?.unsafeCast<Boolean>() ?: false
        }
    }

    open class InputTypeNumberValue(name: String) : InputType<kotlin.Number?>(name) {
        override fun inputValue(event: Event): kotlin.Number? {
            return event.target?.asDynamic()?.valueAsNumber ?: null
        }
    }

    abstract fun inputValue(event: Event): T

    protected fun valueAsString(event: Event): String {
        return event.target?.asDynamic()?.value?.unsafeCast<String>() ?: ""
    }
}

sealed class DirType(val dirStr: String) {
    object Ltr : DirType("ltr")
    object Rtl : DirType("rtl")
    object Auto : DirType("auto")
}

sealed class ATarget(val targetStr: String) {
    object Blank : ATarget("_blank")
    object Parent : ATarget("_parent")
    object Self : ATarget("_self")
    object Top : ATarget("_top")
}

sealed class ARel(val relStr: String) {
    object Alternate : ARel("alternate")
    object Author : ARel("author")
    object Bookmark : ARel("bookmark")
    object External : ARel("external")
    object Help : ARel("help")
    object License : ARel("license")
    object Next : ARel("next")
    object First : ARel("first")
    object Prev : ARel("prev")
    object Last : ARel("last")
    object NoFollow : ARel("nofollow")
    object NoOpener : ARel("noopener")
    object NoReferrer : ARel("noreferrer")
    object Opener : ARel("opener")
    object Search : ARel("search")
    object Tag : ARel("tag")

    class CustomARel(value: String) : ARel(value)
}

enum class Draggable(val str: String) {
    True("true"), False("false"), Auto("auto");
}

enum class ButtonType(val str: String) {
    Button("button"), Reset("reset"), Submit("submit")
}

sealed class ButtonFormTarget(val targetStr: String) {
    object Blank : ButtonFormTarget("_blank")
    object Parent : ButtonFormTarget("_parent")
    object Self : ButtonFormTarget("_self")
    object Top : ButtonFormTarget("_top")
}

enum class ButtonFormMethod(val methodStr: String) {
    Get("get"), Post("post")
}

enum class ButtonFormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class FormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class FormMethod(val methodStr: String) {
    Get("get"),
    Post("post"),
    Dialog("dialog")
}

sealed class FormTarget(val targetStr: String) {
    object Blank : FormTarget("_blank")
    object Parent : FormTarget("_parent")
    object Self : FormTarget("_self")
    object Top : FormTarget("_top")
}

enum class InputFormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class InputFormMethod(val methodStr: String) {
    Get("get"),
    Post("post"),
    Dialog("dialog")
}

sealed class InputFormTarget(val targetStr: String) {
    object Blank : InputFormTarget("_blank")
    object Parent : InputFormTarget("_parent")
    object Self : InputFormTarget("_self")
    object Top : InputFormTarget("_top")
}

enum class TextAreaWrap(val str: String) {
    Hard("hard"),
    Soft("soft"),
    Off("off")
}

enum class Scope(val str: String) {
    Row("row"),
    Rowgroup("rowgroup"),
    Col("col"),
    Colgroup("colgroup")
}
