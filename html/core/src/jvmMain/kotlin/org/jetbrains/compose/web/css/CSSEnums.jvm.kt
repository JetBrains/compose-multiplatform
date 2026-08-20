/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

@JvmInline
private value class JvmLineStyle(private val value: String) : LineStyle {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createLineStyle(value: String): LineStyle = JvmLineStyle(value)

@JvmInline
private value class JvmDisplayStyle(private val value: String) : DisplayStyle {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createDisplayStyle(value: String): DisplayStyle = JvmDisplayStyle(value)

@JvmInline
private value class JvmFlexDirection(private val value: String) : FlexDirection {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createFlexDirection(value: String): FlexDirection = JvmFlexDirection(value)

@JvmInline
private value class JvmFlexWrap(private val value: String) : FlexWrap {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createFlexWrap(value: String): FlexWrap = JvmFlexWrap(value)

@JvmInline
private value class JvmJustifyContent(private val value: String) : JustifyContent {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createJustifyContent(value: String): JustifyContent = JvmJustifyContent(value)

@JvmInline
private value class JvmAlignSelf(private val value: String) : AlignSelf {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAlignSelf(value: String): AlignSelf = JvmAlignSelf(value)

@JvmInline
private value class JvmAlignItems(private val value: String) : AlignItems {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAlignItems(value: String): AlignItems = JvmAlignItems(value)

@JvmInline
private value class JvmAlignContent(private val value: String) : AlignContent {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAlignContent(value: String): AlignContent = JvmAlignContent(value)

@JvmInline
private value class JvmPosition(private val value: String) : Position {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createPosition(value: String): Position = JvmPosition(value)

@JvmInline
private value class JvmStepPosition(private val value: String) : StepPosition {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createStepPosition(value: String): StepPosition = JvmStepPosition(value)

@JvmInline
private value class JvmAnimationTimingFunction(private val value: String) : AnimationTimingFunction {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAnimationTimingFunction(value: String): AnimationTimingFunction =
    JvmAnimationTimingFunction(value)

@JvmInline
private value class JvmAnimationDirection(private val value: String) : AnimationDirection {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAnimationDirection(value: String): AnimationDirection = JvmAnimationDirection(value)

@JvmInline
private value class JvmAnimationFillMode(private val value: String) : AnimationFillMode {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAnimationFillMode(value: String): AnimationFillMode = JvmAnimationFillMode(value)

@JvmInline
private value class JvmAnimationPlayState(private val value: String) : AnimationPlayState {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAnimationPlayState(value: String): AnimationPlayState = JvmAnimationPlayState(value)

@JvmInline
private value class JvmGridAutoFlow(private val value: String) : GridAutoFlow {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createGridAutoFlow(value: String): GridAutoFlow = JvmGridAutoFlow(value)

@JvmInline
private value class JvmVisibilityStyle(private val value: String) : VisibilityStyle {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createVisibilityStyle(value: String): VisibilityStyle = JvmVisibilityStyle(value)
