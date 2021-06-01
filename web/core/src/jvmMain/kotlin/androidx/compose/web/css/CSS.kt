/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED")
package org.jetbrains.compose.web.css

actual interface CSSStyleValue

actual interface CSSKeywordValue : CSSStyleValue {
    actual val value: String

    actual companion object {
        actual operator fun invoke(value: String): CSSKeywordValue = CSSKeywordValueKT(value)
    }
}

actual interface StylePropertyValue {
    actual interface String : StylePropertyValue
    actual interface Number : StylePropertyValue
    actual interface StyleValue : StylePropertyValue
    actual companion object {
        actual operator fun invoke(value: kotlin.String): String = StylePropertyValueJVM(value)

        actual operator fun invoke(value: kotlin.Number): Number = StylePropertyValueJVM(value)

        actual operator fun invoke(value: CSSStyleValue): StyleValue = StylePropertyValueJVM(value)
    }
}

sealed class StylePropertyValueJVM: StylePropertyValue {
    data class String(val value: kotlin.String): StylePropertyValueJVM(), StylePropertyValue.String
    data class Number(val value: kotlin.Number): StylePropertyValueJVM(), StylePropertyValue.Number
    data class StyleValue(val value: CSSStyleValue): StylePropertyValueJVM(), StylePropertyValue.StyleValue

    companion object {
        operator fun invoke(value: kotlin.String) = String(value)

        operator fun invoke(value: kotlin.Number) = Number(value)

        operator fun invoke(value: CSSStyleValue) = StyleValue(value)
    }
}

// type CSSNumeric = number | CSSNumericValue
actual interface CSSNumeric {
    actual interface Number : CSSNumeric
    actual interface NumericValue : CSSNumeric
    actual companion object {
        actual operator fun invoke(value: kotlin.Number): Number {
            TODO("Not yet implemented")
        }

        actual operator fun invoke(value: CSSNumericValue): NumericValue {
            TODO("Not yet implemented")
        }

    }
}

actual val CSSMathValue.operator: CSSMathOperator
    get() = TODO("Not yet implemented")

actual class CSSNumericArray {
    actual fun forEach(handler: (CSSNumericValue) -> Unit) {
    }

    actual val length: Int
        get() = TODO("Not yet implemented")

    actual operator fun get(index: Int): CSSNumericValue {
        TODO("Not yet implemented")
    }

}

actual interface CSSMathSum : CSSMathValue {
    actual val values: CSSNumericArray
}

actual val CSSNumericType.percentHint: CSSNumericBaseType
    get() = TODO("Not yet implemented")

actual interface CSSMathValue : CSSNumericValue {
    actual val strOperator: String
}

actual interface CSSNumericType {
    actual val length: Number
    actual val angle: Number
    actual val time: Number
    actual val frequency: Number
    actual val resolution: Number
    actual val flex: Number
    actual val percent: Number
    actual val strPercentHint: String
}

actual interface CSSNumericValue : CSSStyleValue

actual interface CSSUnitValue : CSSNumericValue {
    actual val value: Number
    actual val unit: String
}

actual interface CSSTypedOM {
    actual fun number(value: Number): CSSnumberValue
    actual fun percent(value: Number): CSSpercentValue
    actual fun em(value: Number): CSSemValue
    actual fun ex(value: Number): CSSexValue
    actual fun ch(value: Number): CSSchValue
    actual fun ic(value: Number): CSSicValue
    actual fun rem(value: Number): CSSremValue
    actual fun lh(value: Number): CSSlhValue
    actual fun rlh(value: Number): CSSrlhValue
    actual fun vw(value: Number): CSSvwValue
    actual fun vh(value: Number): CSSvhValue
    actual fun vi(value: Number): CSSviValue
    actual fun vb(value: Number): CSSvbValue
    actual fun vmin(value: Number): CSSvminValue
    actual fun vmax(value: Number): CSSvmaxValue
    actual fun cm(value: Number): CSScmValue
    actual fun mm(value: Number): CSSmmValue
    actual fun Q(value: Number): CSSQValue
    actual fun pt(value: Number): CSSptValue
    actual fun pc(value: Number): CSSpcValue
    actual fun px(value: Number): CSSpxValue
    actual fun deg(value: Number): CSSdegValue
    actual fun grad(value: Number): CSSgradValue
    actual fun rad(value: Number): CSSradValue
    actual fun turn(value: Number): CSSturnValue
    actual fun s(value: Number): CSSsValue
    actual fun ms(value: Number): CSSmsValue
    actual fun Hz(value: Number): CSSHzValue
    actual fun kHz(value: Number): CSSkHzValue
    actual fun dpi(value: Number): CSSdpiValue
    actual fun dpcm(value: Number): CSSdpcmValue
    actual fun dppx(value: Number): CSSdppxValue
    actual fun fr(value: Number): CSSfrValue

}

actual val CSS: CSSTypedOM
    get() = TODO("Not yet implemented")