/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.intl

/**
 * Plural operands defined in the [Unicode Locale Data Markup Language](https://unicode.org/reports/tr35/tr35-numbers.html#Plural_Operand_Meanings).
 */
internal enum class PluralOperand {
    /**
     * The absolute value of the source number.
     */
    N,

    /**
     * The integer digits of the source number.
     */
    I,

    /**
     * The number of visible fraction digits in the source number, *with* trailing zeros.
     */
    V,

    /**
     * The number of visible fraction digits in the source number, *without* trailing zeros.
     */
    W,

    /**
     * The visible fraction digits in the source number, *with* trailing zeros, expressed as an integer.
     */
    F,

    /**
     * The visible fraction digits in the source number, *without* trailing zeros, expressed as an integer.
     */
    T,

    /**
     * Compact decimal exponent value: exponent of the power of 10 used in compact decimal formatting.
     */
    C,
}