package org.jetbrains.compose.web.css

actual external interface CSSNumericValue<T : CSSUnit> : StylePropertyValue, CSSVariableValueAs<CSSNumericValue<T>>

actual external interface CSSSizeValue<T : CSSUnit> : CSSNumericValue<T> {
    actual val value: Float
    actual val unit: T
}
