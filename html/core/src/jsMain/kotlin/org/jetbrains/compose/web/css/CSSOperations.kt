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
operator fun <T: CSSUnit> CSSSizeValue<T>.unaryMinus(): CSSSizeValue<T> = CSSUnitValueTyped(-value, unit)
operator fun <T: CSSUnit> CSSSizeValue<T>.unaryPlus(): CSSSizeValue<T> = CSSUnitValueTyped(value, unit)

external interface CSSCalcOperation<T : CSSUnit>: CSSNumericValue<T>

data class CSSCalcValue<T : CSSUnit>(
    var op: CSSCalcOperation<out T>
) : CSSCalcOperation<T> {
    override fun toString(): String = "calc$op"
}

private data class CSSPlus<T : CSSUnit>(
    var l: CSSNumericValue<out T>,
    var r: CSSNumericValue<out T>
) : CSSCalcOperation<T> {
    override fun toString(): String = "($l + $r)"
}

private data class CSSMinus<T : CSSUnit>(
    var l: CSSNumericValue<out T>,
    var r: CSSNumericValue<out T>
) : CSSCalcOperation<T> {
    override fun toString(): String = "($l - $r)"
}

private data class CSSTimes<T : CSSUnit>(
    var l: CSSNumericValue<out T>,
    var r: Number,
    val left: Boolean = true
) : CSSCalcOperation<T> {
    override fun toString(): String = if (left) "($l * $r)" else "($r * $l)"
}

private data class CSSDiv<T : CSSUnit>(
    var l: CSSNumericValue<out T>,
    var r: Number
) : CSSCalcOperation<T> {
    override fun toString(): String = "($l / $r)"
}

operator fun <T: CSSUnit> CSSNumericValue<out T>.plus(b: CSSNumericValue<out T>): CSSCalcValue<T> = CSSCalcValue(CSSPlus(this, b))
operator fun <T: CSSUnit> CSSCalcValue<out T>.plus(b: CSSNumericValue<out T>): CSSCalcValue<T> = CSSCalcValue(CSSPlus(this.op, b))
operator fun <T: CSSUnit> CSSNumericValue<out T>.plus(b: CSSCalcValue<out T>): CSSCalcValue<T> = CSSCalcValue(CSSPlus(this, b.op))

operator fun <T: CSSUnit> CSSNumericValue<out T>.minus(b: CSSNumericValue<out T>): CSSCalcValue<T> = CSSCalcValue(CSSMinus(this, b))
operator fun <T: CSSUnit> CSSCalcValue<out T>.minus(b: CSSNumericValue<out T>): CSSCalcValue<T> = CSSCalcValue(CSSMinus(this.op, b))
operator fun <T: CSSUnit> CSSNumericValue<out T>.minus(b: CSSCalcValue<out T>): CSSCalcValue<T> = CSSCalcValue(CSSMinus(this, b.op))

operator fun <T: CSSUnit> CSSCalcValue<out T>.times(b: Number): CSSCalcValue<T> = CSSCalcValue(CSSTimes(this.op, b))
operator fun <T: CSSUnit> Number.times(b: CSSCalcValue<out T>): CSSCalcValue<T> = CSSCalcValue(CSSTimes(b.op, this, false))

operator fun <T: CSSUnit> CSSNumericValue<T>.div(b: Number): CSSCalcValue<T> = CSSCalcValue(CSSDiv(this, b))
operator fun <T: CSSUnit> CSSCalcValue<out T>.div(b: Number): CSSCalcValue<T> = CSSCalcValue(CSSDiv(this.op, b))

operator fun <T: CSSUnit> CSSNumericValue<T>.times(b: Number): CSSCalcValue<T> = CSSCalcValue(CSSTimes(this, b))
operator fun <T: CSSUnit> Number.times(b: CSSNumericValue<T>): CSSCalcValue<T> = CSSCalcValue(CSSTimes(b, this, false))

