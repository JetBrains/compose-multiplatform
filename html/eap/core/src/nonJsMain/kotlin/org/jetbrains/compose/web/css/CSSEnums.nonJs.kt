/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import kotlin.jvm.JvmInline
@PublishedApi
internal actual fun StylePropertyEnum.rawValue(): String = toString()

@JvmInline
private value class LineStyleImpl(private val value: String) : LineStyle {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createLineStyle(value: String): LineStyle = LineStyleImpl(value)

@JvmInline
private value class DisplayStyleImpl(private val value: String) : DisplayStyle {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createDisplayStyle(value: String): DisplayStyle = DisplayStyleImpl(value)

@JvmInline
private value class FlexDirectionImpl(private val value: String) : FlexDirection {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createFlexDirection(value: String): FlexDirection = FlexDirectionImpl(value)

@JvmInline
private value class FlexWrapImpl(private val value: String) : FlexWrap {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createFlexWrap(value: String): FlexWrap = FlexWrapImpl(value)

@JvmInline
private value class JustifyContentImpl(private val value: String) : JustifyContent {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createJustifyContent(value: String): JustifyContent = JustifyContentImpl(value)

@JvmInline
private value class AlignSelfImpl(private val value: String) : AlignSelf {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAlignSelf(value: String): AlignSelf = AlignSelfImpl(value)

@JvmInline
private value class AlignItemsImpl(private val value: String) : AlignItems {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAlignItems(value: String): AlignItems = AlignItemsImpl(value)

@JvmInline
private value class AlignContentImpl(private val value: String) : AlignContent {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAlignContent(value: String): AlignContent = AlignContentImpl(value)

@JvmInline
private value class PositionImpl(private val value: String) : Position {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createPosition(value: String): Position = PositionImpl(value)

@JvmInline
private value class StepPositionImpl(private val value: String) : StepPosition {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createStepPosition(value: String): StepPosition = StepPositionImpl(value)

@JvmInline
private value class AnimationTimingFunctionImpl(private val value: String) : AnimationTimingFunction {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAnimationTimingFunction(value: String): AnimationTimingFunction =
    AnimationTimingFunctionImpl(value)

@JvmInline
private value class AnimationDirectionImpl(private val value: String) : AnimationDirection {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAnimationDirection(value: String): AnimationDirection = AnimationDirectionImpl(value)

@JvmInline
private value class AnimationFillModeImpl(private val value: String) : AnimationFillMode {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAnimationFillMode(value: String): AnimationFillMode = AnimationFillModeImpl(value)

@JvmInline
private value class AnimationPlayStateImpl(private val value: String) : AnimationPlayState {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createAnimationPlayState(value: String): AnimationPlayState = AnimationPlayStateImpl(value)

@JvmInline
private value class GridAutoFlowImpl(private val value: String) : GridAutoFlow {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createGridAutoFlow(value: String): GridAutoFlow = GridAutoFlowImpl(value)

@JvmInline
private value class VisibilityStyleImpl(private val value: String) : VisibilityStyle {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createVisibilityStyle(value: String): VisibilityStyle = VisibilityStyleImpl(value)
