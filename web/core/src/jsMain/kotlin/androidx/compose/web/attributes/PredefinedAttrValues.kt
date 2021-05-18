package androidx.compose.web.attributes

sealed class InputType(val typeStr: String) {
    object Button : InputType("button")
    object Checkbox : InputType("checkbox")
    object Color : InputType("color")
    object Date : InputType("date")
    object DateTimeLocal : InputType("datetime-local")
    object Email : InputType("email")
    object File : InputType("file")
    object Hidden : InputType("hidden")
    object Month : InputType("month")
    object Number : InputType("number")
    object Password : InputType("password")
    object Radio : InputType("radio")
    object Range : InputType("range")
    object Search : InputType("search")
    object Submit : InputType("submit")
    object Tel : InputType("tel")
    object Text : InputType("text")
    object Time : InputType("time")
    object Url : InputType("url")
    object Week : InputType("week")
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
