/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CompilerPluginTestHarnessTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun comparesPostPluginIrWithGolden() {
        val module = PluginTestModule(
            "main",
            listOf(KotlinSource("Test.kt", "fun answer(): Int = 1")),
            registrars = listOf(ReplaceOnePlugin()),
            configure = { put(REPLACEMENT, 42) },
        )
        val harness = CompilerPluginTestHarness()
        val text = harness.render(module)
        assertTrue(text.substringAfter("// Transformed IR:").contains("42"))
        assertFalse(text.contains("kotlin-plugin-test-"))
        assertEquals(text, harness.render(module))
        val mode = if (System.getProperty("kotlin.plugin.tests.update.goldens") == "true") GoldenMode.UPDATE else GoldenMode.VERIFY
        GoldenFiles.assertMatches(File("testData/replaceOne.txt"), text, mode)
    }

    @Test
    fun compilesDependenciesWithTheirOwnPluginsAndConfiguration() {
        val library = PluginTestModule(
            "library",
            listOf(KotlinSource("Library.kt", "package lib\nconst val VALUE: Int = 1")),
            registrars = listOf(ReplaceOnePlugin()),
            configure = { put(REPLACEMENT, 17) },
        )
        val bridge = PluginTestModule(
            "bridge",
            listOf(KotlinSource("Bridge.kt", "fun bridge(): Int = lib.VALUE")),
            dependencies = listOf(library),
        )
        val module = PluginTestModule(
            "main",
            listOf(KotlinSource("Test.kt", "fun answer(): Int = lib.VALUE + bridge()")),
            dependencies = listOf(bridge),
        )
        val output = CompilerPluginTestHarness().render(module)
        assertTrue(output.substringAfter("// Transformed IR:").contains("17"), output)
    }

    @Test
    fun supportsFilesMultipleSourcesAndCustomRenderer() {
        val source = File(directory, "Test.kt").apply { writeText("fun answer() = helper()") }
        val module = PluginTestModule(
            "main", listOf(KotlinSource.fromFile(source), KotlinSource("other/Helper.kt", "fun helper() = 1")),
        )
        val output = CompilerPluginTestHarness { "files=${it.files.size}" }.render(module)
        assertTrue(output.endsWith("files=2"))
        assertTrue(output.contains("// Source: main/other/Helper.kt"))
    }

    @Test
    fun includesStandardLibraryByDefault() {
        val module = PluginTestModule(
            "main",
            listOf(KotlinSource("Test.kt", "fun values() = listOf(1, 2).size")),
        )
        assertTrue(CompilerPluginTestHarness().render(module).contains("listOf"))
    }

    @Test
    fun standardLibraryCanBeOmitted() {
        val module = PluginTestModule(
            "main",
            listOf(KotlinSource("Test.kt", "fun values() = listOf(1, 2).size")),
            includeStdlib = false,
        )
        val failure = assertThrows(AssertionError::class.java) { CompilerPluginTestHarness().render(module) }
        assertTrue(failure.message!!.contains("listOf"), failure.message)
    }

    @Test
    fun standardLibraryCanBeSuppliedExplicitly() {
        val stdlib = File(Unit::class.java.protectionDomain.codeSource.location.toURI())
        val module = PluginTestModule(
            "main",
            listOf(KotlinSource("Test.kt", "fun values() = listOf(1, 2).size")),
            classpath = listOf(stdlib),
            includeStdlib = false,
        )
        assertTrue(CompilerPluginTestHarness().render(module).contains("listOf"))
    }

    @Test
    fun additionalLibrariesMustStillBeSuppliedExplicitly() {
        val source = KotlinSource("Test.kt", "fun mode() = org.jetbrains.kotlin.plugin.testing.GoldenMode.VERIFY")
        val harness = CompilerPluginTestHarness()
        val failure = assertThrows(AssertionError::class.java) {
            harness.render(PluginTestModule("main", listOf(source)))
        }
        assertTrue(failure.message!!.contains("Unresolved reference"), failure.message)

        val library = File(GoldenMode::class.java.protectionDomain.codeSource.location.toURI())
        val module = PluginTestModule("main", listOf(source), classpath = listOf(library))
        assertTrue(harness.render(module).contains("GoldenMode"))
    }

    @Test
    fun compilationFailureDoesNotUpdateGolden() {
        val module = PluginTestModule("broken", listOf(KotlinSource("Broken.kt", "fun broken() = doesNotExist()")))
        val golden = File(directory, "golden.txt").apply { writeText("keep me") }
        val failure = assertThrows(AssertionError::class.java) {
            CompilerPluginTestHarness().verify(module, golden, GoldenMode.UPDATE)
        }
        assertTrue(failure.message!!.contains("broken"))
        assertTrue(failure.message!!.contains("doesNotExist"))
        assertEquals("keep me", golden.readText())
    }

    @Test
    fun rejectsDuplicateModuleNamesAndUnsafeSourceNames() {
        assertThrows(IllegalArgumentException::class.java) { KotlinSource("../Test.kt", "") }
        val library = PluginTestModule("same", listOf(KotlinSource("Lib.kt", "")))
        val root = PluginTestModule("same", listOf(KotlinSource("Test.kt", "")), dependencies = listOf(library))
        assertThrows(IllegalArgumentException::class.java) { CompilerPluginTestHarness().render(root) }
    }

    @Test
    fun aFailedRendererDoesNotPoisonTheNextCompilation() {
        val module = PluginTestModule("main", listOf(KotlinSource("Test.kt", "fun answer() = 1")))
        assertThrows(AssertionError::class.java) {
            CompilerPluginTestHarness { error("renderer failed") }.render(module)
        }
        assertTrue(CompilerPluginTestHarness().render(module).contains("answer"))
    }

    private class ReplaceOnePlugin : CompilerPluginRegistrar() {
        override val pluginId: String = "test.replace.one"
        override val supportsK2: Boolean = true

        override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
            val replacement = configuration[REPLACEMENT] ?: 42
            IrGenerationExtension.registerExtension(object : IrGenerationExtension {
                override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
                    moduleFragment.transformChildren(object : IrElementTransformerVoid() {
                        override fun visitConst(expression: IrConst): IrExpression =
                            if (expression.value == 1) {
                                IrConstImpl.int(expression.startOffset, expression.endOffset, expression.type, replacement)
                            } else {
                                expression
                            }
                    }, null)
                }
            })
        }
    }

    companion object {
        private val REPLACEMENT = CompilerConfigurationKey.create<Int>("replacement constant")
    }
}
