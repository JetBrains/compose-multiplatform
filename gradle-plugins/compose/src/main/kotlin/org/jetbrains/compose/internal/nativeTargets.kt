package org.jetbrains.compose.internal

import org.jetbrains.kotlin.konan.target.KonanTarget

internal val SUPPORTED_NATIVE_TARGETS = setOf(
    KonanTarget.IOS_ARM32,
    KonanTarget.IOS_X64,
    KonanTarget.IOS_ARM64,
    KonanTarget.IOS_SIMULATOR_ARM64,
    KonanTarget.MACOS_X64,
    KonanTarget.MACOS_ARM64,
)