@file:Suppress("UNUSED")
package org.jetbrains.compose.web.css

interface CSSAutoValue : CSSStyleValue

val auto = StylePropertyValue("auto").unsafeCast<CSSAutoValue>()

// type CSSSizeOrAutoValue = CSSSizeValue | CSSAutoValue
interface CSSSizeOrAutoValue : CSSStyleValue, StylePropertyValue {
    companion object {
        operator fun invoke(value: CSSSizeValue) = value.unsafeCast<CSSSizeOrAutoValue>()
        operator fun invoke(value: CSSAutoValue) = value.unsafeCast<CSSSizeOrAutoValue>()
    }
}

fun CSSSizeOrAutoValue.asCSSSizeValue() = this.asDynamic() as? CSSUnitValueJS

enum class Direction {
    rtl,
    ltr;

    override fun toString(): String = this.name
}

typealias LanguageCode = String
