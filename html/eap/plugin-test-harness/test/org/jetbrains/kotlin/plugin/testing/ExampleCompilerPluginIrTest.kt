/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.junit.jupiter.api.Test
import java.io.File

/** Copy this test and its golden, then replace the registrar and source with your own. */
@OptIn(ExperimentalCompilerApi::class)
class ExampleCompilerPluginIrTest : AbstractCompilerPluginIrTest() {
    override val registrars: List<CompilerPluginRegistrar> = listOf(ReplaceOnePlugin())

    @Test
    fun replacesOneWithFortyTwo() {
        verify(
            module(
                sources = listOf(KotlinSource("Example.kt", "fun answer(): Int = 1")),
            ),
            goldenFile = File("testData/example.txt"),
            // To regenerate the golden, temporarily add mode = GoldenMode.UPDATE and review the result.
        )
    }

    // This sample plugin is self-contained; use your real plugin's registrar instead.
    private class ReplaceOnePlugin : CompilerPluginRegistrar() {
        override val pluginId: String = "example.replace.one"
        override val supportsK2: Boolean = true

        override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
            IrGenerationExtension.registerExtension(object : IrGenerationExtension {
                override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
                    moduleFragment.transformChildren(object : IrElementTransformerVoid() {
                        override fun visitConst(expression: IrConst): IrExpression =
                            if (expression.value == 1) {
                                IrConstImpl.int(expression.startOffset, expression.endOffset, expression.type, 42)
                            } else {
                                expression
                            }
                    }, null)
                }
            })
        }
    }
}
