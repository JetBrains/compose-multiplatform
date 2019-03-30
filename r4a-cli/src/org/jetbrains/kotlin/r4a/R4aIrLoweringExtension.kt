/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.extensions.IrLoweringExtension
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.r4a.compiler.lower.R4aObservePatcher
import org.jetbrains.kotlin.r4a.frames.FrameIrTransformer

val R4aObservePhase = makeIrFilePhase(
    ::R4aObservePatcher,
    name = "R4aObservePhase",
    description = "Observe @Model"
)

val FrameClassGenPhase = makeIrFilePhase(
    ::FrameIrTransformer,
    name = "R4aFrameTransformPhase",
    description = "Transform @Model classes into framed classes"
)

class R4aIrLoweringExtension : IrLoweringExtension {
    override fun interceptLoweringPhases(phases: CompilerPhase<JvmBackendContext, IrFile, IrFile>): CompilerPhase<JvmBackendContext, IrFile, IrFile> {
        return FrameClassGenPhase then R4aObservePhase then phases
    }
}

