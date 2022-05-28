@file:Suppress("Unused", "NOTHING_TO_INLINE", "NESTED_CLASS_IN_EXTERNAL_INTERFACE", "INLINE_EXTERNAL_DECLARATION", "WRONG_BODY_OF_EXTERNAL_DECLARATION", "NESTED_EXTERNAL_DECLARATION")

package org.jetbrains.compose.web.css

interface VisibilityStyle: StylePropertyEnum {
    companion object {
        inline val Visible get() = VisibilityStyle("visible")
        inline val Hidden get() = VisibilityStyle("hidden")
        inline val Collapse get() = VisibilityStyle("collapse")


        inline val Inherit get() = VisibilityStyle("inherit")
        inline val Initial get() = VisibilityStyle("initial")

        inline val Revert get() = VisibilityStyle("revert")
        inline val RevertLayer get() = VisibilityStyle("revert-layer")

        inline val Unset get() = VisibilityStyle("unset")
    }
}
inline fun VisibilityStyle(value: String) = value.unsafeCast<VisibilityStyle>()

fun StyleScope.visibility(visibilityStyle: VisibilityStyle) {
    property("visibility", visibilityStyle.value)
}
