/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.css

actual interface CSSSizeOrAutoValue : CSSStyleValue, StylePropertyValue {
    actual interface Size : CSSSizeOrAutoValue
    actual interface Auto : CSSSizeOrAutoValue
    actual companion object {
        actual operator fun invoke(value: CSSSizeValue): Size {
            TODO("Not yet implemented")
        }

        actual operator fun invoke(value: CSSAutoValue): Auto {
            TODO("Not yet implemented")
        }
    }
}