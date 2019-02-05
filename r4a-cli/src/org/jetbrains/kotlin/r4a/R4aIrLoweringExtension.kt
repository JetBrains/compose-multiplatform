/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.backend.common.CompilerPhase
import org.jetbrains.kotlin.backend.common.makePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.extensions.IrLoweringExtension
import org.jetbrains.kotlin.backend.jvm.makePatchParentsPhase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.r4a.compiler.lower.R4aObservePatcher

class R4aIrLoweringExtension : IrLoweringExtension {
    override fun interceptLoweringPhases(phases: List<CompilerPhase<JvmBackendContext, IrFile>>): List<CompilerPhase<JvmBackendContext, IrFile>> {
        val phases = phases.toMutableList()
        phases.add(1, makePatchParentsPhase(0))
        phases.add(2, makePhase<JvmBackendContext, IrFile>(
            { context, file -> R4aObservePatcher(context).lower(file) },
            "R4aObserve",
            "Insert calls to Observe in composable functions",
            emptySet<CompilerPhase<JvmBackendContext, IrFile>>()
        ))

        return phases
    }
}

