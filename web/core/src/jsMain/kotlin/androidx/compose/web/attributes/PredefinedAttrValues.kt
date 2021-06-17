package org.jetbrains.compose.web.attributes

import org.w3c.dom.events.Event

sealed class InputType<T>(val typeStr: String) {

    object Button : InputType<Unit>("button") {
        override fun inputValue(event: Event) = Unit
    }

    object Checkbox : InputType<Boolean>("checkbox") {
        override fun inputValue(event: Event): Boolean {
            return event.target?.asDynamic()?.checked?.unsafeCast<Boolean>() ?: false
        }
    }

    object Color : InputType<String>("color") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Date : InputType<String>("date") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object DateTimeLocal : InputType<String>("datetime-local") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Email : InputType<String>("email") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object File : InputType<String>("file") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Hidden : InputType<String>("hidden") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Month : InputType<String>("month") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Number : InputType<kotlin.Number?>("number") {
        override fun inputValue(event: Event): kotlin.Number? {
            return event.target?.asDynamic()?.value ?: null
        }
    }

    object Password : InputType<String>("password") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Radio : InputType<Boolean>("radio") {
        override fun inputValue(event: Event): Boolean {
            return event.target?.asDynamic()?.checked?.unsafeCast<Boolean>() ?: false
        }
    }

    object Range : InputType<kotlin.Number?>("range") {
        override fun inputValue(event: Event): kotlin.Number? {
            return event.target?.asDynamic()?.valueAsNumber ?: null
        }
    }

    object Search : InputType<String>("search") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Submit : InputType<Unit>("submit") {
        override fun inputValue(event: Event) = Unit
    }

    object Tel : InputType<String>("tel") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Text : InputType<String>("text") {
        override fun inputValue(event: Event) = valueAsString(event)
    }

    object Time : InputType<String>("time") {
        override fun inputValue(event: Event) = Search.valueAsString(event)
    }

    object Url : InputType<String>("url") {
        override fun inputValue(event: Event) = valueAsString(event)
    }
    object Week : InputType<String>("week") {
        override fun inputValue(event: Event) = valueAsString(event)
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
