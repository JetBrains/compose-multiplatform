/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

operator fun <T: CSSUnit> CSSSizeValue<T>.times(num: Number): CSSSizeValue<T> = CSSUnitValueTyped(value * num.toFloat(), unit)
operator fun <T: CSSUnit> Number.times(unit: CSSSizeValue<T>): CSSSizeValue<T> = CSSUnitValueTyped(unit.value * toFloat(), unit.unit)

operator fun <T: CSSUnit> CSSSizeValue<T>.div(num: Number): CSSSizeValue<T> = CSSUnitValueTyped(value / num.toFloat(), unit)

operator fun <T: CSSUnit> CSSSizeValue<T>.plus(b: CSSSizeValue<T>): CSSSizeValue<T> = CSSUnitValueTyped(value + b.value, unit)
operator fun <T: CSSUnit> CSSSizeValue<T>.minus(b: CSSSizeValue<T>): CSSSizeValue<T> = CSSUnitValueTyped(value - b.value, unit)