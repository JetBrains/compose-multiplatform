/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.internal.unsafeCast

@PublishedApi
internal actual fun createLineStyle(value: String): LineStyle = value.unsafeCast<LineStyle>()

@PublishedApi
internal actual fun createDisplayStyle(value: String): DisplayStyle = value.unsafeCast<DisplayStyle>()

@PublishedApi
internal actual fun createFlexDirection(value: String): FlexDirection = value.unsafeCast<FlexDirection>()

@PublishedApi
internal actual fun createFlexWrap(value: String): FlexWrap = value.unsafeCast<FlexWrap>()

@PublishedApi
internal actual fun createJustifyContent(value: String): JustifyContent = value.unsafeCast<JustifyContent>()

@PublishedApi
internal actual fun createAlignSelf(value: String): AlignSelf = value.unsafeCast<AlignSelf>()

@PublishedApi
internal actual fun createAlignItems(value: String): AlignItems = value.unsafeCast<AlignItems>()

@PublishedApi
internal actual fun createAlignContent(value: String): AlignContent = value.unsafeCast<AlignContent>()

@PublishedApi
internal actual fun createPosition(value: String): Position = value.unsafeCast<Position>()

@PublishedApi
internal actual fun createStepPosition(value: String): StepPosition = value.unsafeCast<StepPosition>()

@PublishedApi
internal actual fun createAnimationTimingFunction(value: String): AnimationTimingFunction =
    value.unsafeCast<AnimationTimingFunction>()

@PublishedApi
internal actual fun createAnimationDirection(value: String): AnimationDirection = value.unsafeCast<AnimationDirection>()

@PublishedApi
internal actual fun createAnimationFillMode(value: String): AnimationFillMode = value.unsafeCast<AnimationFillMode>()

@PublishedApi
internal actual fun createAnimationPlayState(value: String): AnimationPlayState = value.unsafeCast<AnimationPlayState>()

@PublishedApi
internal actual fun createGridAutoFlow(value: String): GridAutoFlow = value.unsafeCast<GridAutoFlow>()

@PublishedApi
internal actual fun createVisibilityStyle(value: String): VisibilityStyle = value.unsafeCast<VisibilityStyle>()
