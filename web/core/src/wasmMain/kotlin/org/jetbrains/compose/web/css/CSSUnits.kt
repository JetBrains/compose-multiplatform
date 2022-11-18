package org.jetbrains.compose.web.css

actual interface CSSNumericValue<T : CSSUnit> : StylePropertyValue, CSSVariableValueAs<CSSNumericValue<T>>

actual interface CSSSizeValue<T : CSSUnit> : CSSNumericValue<T> {
    actual val value: Float
    actual val unit: T
}
