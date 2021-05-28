/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED")
package androidx.compose.web.css

import kotlin.js.JsName

expect interface CSSStyleValue

expect interface CSSKeywordValue : CSSStyleValue {
    val value: String

    companion object {
        operator fun invoke(value: String): CSSKeywordValue
    }
}

open class CSSKeywordValueKT(override val value: String) : CSSKeywordValue

// type StylePropertyValue = string | number | CSSStyleValue
expect interface StylePropertyValue {
    interface String: StylePropertyValue
    interface Number: StylePropertyValue
    interface StyleValue: StylePropertyValue
    companion object {
        operator fun invoke(value: kotlin.String): String

        operator fun invoke(value: kotlin.Number): Number

        operator fun invoke(value: CSSStyleValue): StyleValue
    }
}

// type CSSNumeric = number | CSSNumericValue
expect interface CSSNumeric {
    interface Number: CSSNumeric
    interface NumericValue: CSSNumeric
    companion object {
        operator fun invoke(value: kotlin.Number): Number

        operator fun invoke(value: CSSNumericValue): NumericValue
    }
}

enum class CSSMathOperator(val value: String) {
    sum("sum"),
    product("product"),
    negate("negate"),
    invert("invert"),
    min("min"),
    max("max"),
    clamp("clamp")
}

expect interface CSSMathValue : CSSNumericValue {
    val strOperator: String
}

expect val CSSMathValue.operator: CSSMathOperator

expect class CSSNumericArray {
    fun forEach(handler: (CSSNumericValue) -> Unit)
    val length: Int

    operator fun get(index: Int): CSSNumericValue
}

expect interface CSSMathSum : CSSMathValue {
    val values: CSSNumericArray
}

expect interface CSSNumericType {
    val length: Number
    val angle: Number
    val time: Number
    val frequency: Number
    val resolution: Number
    val flex: Number
    val percent: Number
    val strPercentHint: String
}

enum class CSSNumericBaseType(val value: String) {
    @JsName("_length")
    length("length"),
    angle("angle"),
    time("time"),
    frequency("frequency"),
    resolution("resolution"),
    flex("flex"),
    percent("percent")
}

expect val CSSNumericType.percentHint: CSSNumericBaseType

expect interface CSSNumericValue : CSSStyleValue

expect interface CSSUnitValue : CSSNumericValue {
    val value: Number
    val unit: String
}

expect interface CSSTypedOM {
    fun number(value: Number): CSSnumberValue
    fun percent(value: Number): CSSpercentValue

    // <length>
    fun em(value: Number): CSSemValue
    fun ex(value: Number): CSSexValue
    fun ch(value: Number): CSSchValue
    fun ic(value: Number): CSSicValue
    fun rem(value: Number): CSSremValue
    fun lh(value: Number): CSSlhValue
    fun rlh(value: Number): CSSrlhValue
    fun vw(value: Number): CSSvwValue
    fun vh(value: Number): CSSvhValue
    fun vi(value: Number): CSSviValue
    fun vb(value: Number): CSSvbValue
    fun vmin(value: Number): CSSvminValue
    fun vmax(value: Number): CSSvmaxValue
    fun cm(value: Number): CSScmValue
    fun mm(value: Number): CSSmmValue
    fun Q(value: Number): CSSQValue

    fun pt(value: Number): CSSptValue
    fun pc(value: Number): CSSpcValue
    fun px(value: Number): CSSpxValue

    // <angle>
    fun deg(value: Number): CSSdegValue
    fun grad(value: Number): CSSgradValue
    fun rad(value: Number): CSSradValue
    fun turn(value: Number): CSSturnValue

    // <time>
    fun s(value: Number): CSSsValue
    fun ms(value: Number): CSSmsValue

    // <frequency>
    fun Hz(value: Number): CSSHzValue
    fun kHz(value: Number): CSSkHzValue

    // <resolution>
    fun dpi(value: Number): CSSdpiValue
    fun dpcm(value: Number): CSSdpcmValue
    fun dppx(value: Number): CSSdppxValue

    // <flex>
    fun fr(value: Number): CSSfrValue
}

expect val CSS: CSSTypedOM
