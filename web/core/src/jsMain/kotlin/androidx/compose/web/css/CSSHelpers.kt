@file:Suppress("UNUSED")
package org.jetbrains.compose.web.css

interface CSSAutoValue : CSSSizeOrAutoValue

val auto = "auto".unsafeCast<CSSAutoValue>()

external interface CSSSizeOrAutoValue : StylePropertyValue

enum class Direction {
    rtl,
    ltr;

    override fun toString(): String = this.name
}

typealias LanguageCode = String
