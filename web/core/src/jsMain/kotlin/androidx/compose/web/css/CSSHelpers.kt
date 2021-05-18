/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("UNUSED")

package androidx.compose.web.css

interface CSSAutoValue : CSSKeywordValue

val auto = CSSKeywordValueJS("auto").unsafeCast<CSSAutoValue>()
fun asCSSAutoValue(value: dynamic) = (value as? CSSKeywordValueJS).unsafeCast<CSSAutoValue>()

// type CSSSizeOrAutoValue = CSSSizeValue | CSSAutoValue
interface CSSSizeOrAutoValue : CSSStyleValue, StylePropertyValue {
    companion object {
        operator fun invoke(value: CSSSizeValue) = value.unsafeCast<CSSSizeOrAutoValue>()
        operator fun invoke(value: CSSAutoValue) = value.unsafeCast<CSSSizeOrAutoValue>()
    }
}

fun CSSSizeOrAutoValue.asCSSSizeValue() = this.asDynamic() as? CSSUnitValueJS
fun CSSSizeOrAutoValue.asCSSAutoValue(): CSSAutoValue = asCSSAutoValue(this.asDynamic())

enum class Direction {
    rtl,
    ltr;

    override fun toString(): String = this.name
}

typealias LanguageCode = String