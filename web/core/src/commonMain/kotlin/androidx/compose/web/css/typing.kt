/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

expect interface CSSSizeOrAutoValue : CSSStyleValue, StylePropertyValue {
    interface Size : CSSSizeOrAutoValue
    interface Auto : CSSSizeOrAutoValue
    companion object {
        operator fun invoke(value: CSSSizeValue): Size
        operator fun invoke(value: CSSAutoValue): Auto
    }
}

enum class Direction {
    rtl,
    ltr;

    override fun toString(): String = this.name
}

typealias LanguageCode = String
