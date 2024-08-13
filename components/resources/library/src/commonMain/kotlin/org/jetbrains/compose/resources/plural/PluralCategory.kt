/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.plural

/**
 * Plural categories defined in the [CLDR Language Plural Rules](https://cldr.unicode.org/index/cldr-spec/plural-rules).
 */
internal enum class PluralCategory {
    ZERO,
    ONE,
    TWO,
    FEW,
    MANY,
    OTHER;

    companion object {
        fun fromString(name: String): PluralCategory? {
            return entries.firstOrNull {
                it.name.equals(name, true)
            }
        }
    }
}
