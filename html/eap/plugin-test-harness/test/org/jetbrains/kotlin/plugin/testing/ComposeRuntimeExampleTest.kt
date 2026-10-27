/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import androidx.compose.runtime.Composable
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

/** An example with a custom test registrar and Compose runtime as an explicit external library. */
@OptIn(ExperimentalCompilerApi::class)
class ComposeRuntimeExampleTest : AbstractCompilerPluginIrTest() {
    override val registrars: List<CompilerPluginRegistrar> = listOf(TestRegistrar())
    override val additionalClasspath = listOf(classpathEntryFor<Composable>())

    @Test
    fun replacesConstantInComposableFunction() {
        verify(
            module(
                sources = listOf(
                    KotlinSource(
                        "Example.kt",
                        """
                            import androidx.compose.runtime.Composable

                            @Composable
                            fun answer(): Int = 1
                        """.trimIndent(),
                    ),
                ),
            ),
            goldenFile = File("testData/composeRuntime.txt"),
            // To regenerate the golden, temporarily add mode = GoldenMode.UPDATE and review the result.
        )
    }

    // Compose supplies the annotation; this registrar performs the IR transformation.
    private class TestRegistrar : CompilerPluginRegistrar() {
        override val pluginId: String = "test.compose.runtime.replace.one"
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
