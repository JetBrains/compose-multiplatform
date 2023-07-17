/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal.utils

import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

internal fun KotlinNativeTarget.isIosSimulatorTarget(): Boolean =
    konanTarget === KonanTarget.IOS_X64 || konanTarget === KonanTarget.IOS_SIMULATOR_ARM64

internal fun KotlinNativeTarget.isIosDeviceTarget(): Boolean =
    konanTarget === KonanTarget.IOS_ARM64 || konanTarget === KonanTarget.IOS_ARM32

internal fun KotlinNativeTarget.isIosTarget(): Boolean =
    isIosSimulatorTarget() || isIosDeviceTarget()

internal fun KotlinTarget.asIosNativeTargetOrNull(): KotlinNativeTarget? =
    (this as? KotlinNativeTarget)?.takeIf { it.isIosTarget() }
