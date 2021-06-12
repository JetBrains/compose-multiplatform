@file:Suppress("UNUSED")
package org.jetbrains.compose.web.css

interface CSSAutoValue : StylePropertyValue

val auto = "auto".unsafeCast<CSSAutoValue>()

enum class Direction {
    rtl,
    ltr;

    override fun toString(): String = this.name
}

typealias LanguageCode = String
