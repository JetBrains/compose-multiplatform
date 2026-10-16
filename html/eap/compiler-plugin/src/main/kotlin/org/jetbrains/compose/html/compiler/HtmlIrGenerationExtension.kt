/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class HtmlIrGenerationExtension(
    private val configuration: CompilerConfiguration
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        configuration.reportInfo(
            "[compose-html-lowering] IR for ${moduleFragment.name}: " +
                "${moduleFragment.files.size} source file(s)"
        )
        val symbols = HtmlSymbols.resolve(pluginContext.finderForBuiltins()) ?: return
        val stringRenderingFunctions = findStringRenderingFunctions(moduleFragment, symbols.composeHtmlToStringSymbol)
        moduleFragment.transformChildrenVoid(
            StaticHtmlTransformer(pluginContext, configuration, symbols, stringRenderingFunctions)
        )
    }
}
