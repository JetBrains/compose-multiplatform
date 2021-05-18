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

package androidx.compose.web.css

abstract class Color : CustomStyleValue {
    override fun styleValue(): StylePropertyValue = StylePropertyValue(toString())

    data class Named(val value: String) : Color() {
        override fun toString(): String = value
    }

    data class RGB(val r: Number, val g: Number, val b: Number) : Color() {
        override fun toString(): String = "rgb($r, $g, $b)"
    }

    data class RGBA(val r: Number, val g: Number, val b: Number, val a: Number) : Color() {
        override fun toString(): String = "rgba($r, $g, $b, $a)"
    }

    data class HSL(val h: CSSangleValue, val s: Number, val l: Number) : Color() {
        constructor(h: Number, s: Number, l: Number) : this(h.deg, s, l)

        override fun toString(): String = "hsl($h, $s%, $l%)"
    }

    data class HSLA(val h: CSSangleValue, val s: Number, val l: Number, val a: Number) : Color() {
        constructor(h: Number, s: Number, l: Number, a: Number) : this(h.deg, s, l, a)

        override fun toString(): String = "hsla($h, $s%, $l%, $a)"
    }

    companion object {
        operator fun invoke(name: String) = Named(name)
    }
}
