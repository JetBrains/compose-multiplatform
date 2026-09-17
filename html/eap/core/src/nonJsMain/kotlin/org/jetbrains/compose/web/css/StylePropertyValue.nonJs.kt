/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import kotlin.jvm.JvmInline
import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword

actual interface StylePropertyValue

actual interface StylePropertyNumber : StylePropertyValue
actual interface StylePropertyString : StylePropertyValue

actual interface CSSStyleValue : StylePropertyValue {
    actual override fun toString(): String
}

actual interface CSSVariableValueAs<out T : StylePropertyValue>

@JvmInline
private value class StylePropertyStringImpl(
    private val value: String,
) : StylePropertyString {
    override fun toString(): String = value
}

@JvmInline
private value class StylePropertyNumberImpl(
    private val value: Number,
) : StylePropertyNumber {
    override fun toString(): String = formatCssNumber(value)
}

@JvmInline
private value class CSSStyleValueImpl(
    private val value: String,
) : CSSStyleValue {
    override fun toString(): String = value
}

// References implement the marker types they can represent without pretending
// to be a CSSSizeValue, whose value and unit members do not exist for var().
@JvmInline
private value class CSSVariableReferenceImpl(
    private val value: String,
) : CSSNumericValue<CSSUnit>,
    LineStyle,
    DisplayStyle,
    FlexDirection,
    FlexWrap,
    JustifyContent,
    AlignSelf,
    AlignItems,
    AlignContent,
    Position,
    StepPosition,
    AnimationTimingFunction,
    AnimationDirection,
    AnimationFillMode,
    AnimationPlayState,
    GridAutoFlow,
    VisibilityStyle,
    StylePropertyNumber,
    StylePropertyString,
    CSSAutoKeyword {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createStylePropertyString(value: String): StylePropertyString =
    StylePropertyStringImpl(value)

@PublishedApi
internal actual fun createStylePropertyNumber(value: Number): StylePropertyNumber =
    StylePropertyNumberImpl(value)

internal actual fun createCSSVariableReference(cssText: String): StylePropertyValue =
    CSSVariableReferenceImpl(cssText)

@PublishedApi
internal actual fun createCSSStyleValue(value: String): CSSStyleValue =
    CSSStyleValueImpl(value)
