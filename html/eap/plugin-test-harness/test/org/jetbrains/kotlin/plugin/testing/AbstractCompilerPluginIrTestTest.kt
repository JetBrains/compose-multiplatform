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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AbstractCompilerPluginIrTestTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun usesAutomaticStdlibAndExplicitGoldenModes() {
        val fixture = Fixture()
        val golden = File(directory, "example.txt")
        assertThrows(AssertionError::class.java) { fixture.check(golden) }
        assertFalse(golden.exists())
        assertTrue(File(golden.path + ".actual").isFile)
        fixture.check(golden, mode = GoldenMode.UPDATE)
        fixture.check(golden)
        assertTrue(golden.readText().contains("// Transformed IR: main"))
        assertFalse(File(golden.path + ".actual").exists())
    }

    @Test
    fun combinesClassDefaultsWithPerTestClasspath() {
        val fixture = Fixture(
            sources = listOf(
                KotlinSource(
                    "Test.kt",
                    """
                        fun mode() = org.jetbrains.kotlin.plugin.testing.GoldenMode.VERIFY
                        fun marker() = org.jetbrains.kotlin.plugin.testing.AbstractCompilerPluginIrTestTest()
                    """.trimIndent(),
                ),
            ),
            additionalClasspath = listOf(classpathEntryFor<GoldenMode>()),
        )
        val golden = File(directory, "classpath.txt")
        assertThrows(AssertionError::class.java) { fixture.check(golden, mode = GoldenMode.UPDATE) }
        fixture.check(
            golden,
            mode = GoldenMode.UPDATE,
            extraClasspath = listOf(classpathEntryFor<AbstractCompilerPluginIrTestTest>()),
        )
        assertTrue(golden.readText().contains("GoldenMode"))
        assertTrue(golden.readText().contains("AbstractCompilerPluginIrTestTest"))
    }

    @Test
    fun appliesConfigurationBeforePerTestOverridesAndUsesCustomRenderer() {
        val calls = mutableListOf<Pair<String, Int?>>()
        val fixture = Fixture(
            registrars = listOf(RecordingPlugin(calls)),
            renderer = IrRenderer { "custom rendering" },
        )
        val golden = File(directory, "renderer.txt")
        fixture.check(golden, mode = GoldenMode.UPDATE) { put(SETTING, 9) }
        assertEquals(listOf("<main>" to 9), calls)
        assertTrue(golden.readText().endsWith("custom rendering\n"))
    }

    @Test
    fun doesNotApplyClassDefaultsToExplicitTestModule() {
        val calls = mutableListOf<Pair<String, Int?>>()
        val fixture = Fixture(registrars = listOf(RecordingPlugin(calls)))
        val module = PluginTestModule(
            name = "explicit",
            sources = listOf(KotlinSource("Test.kt", "fun answer() = 1")),
            registrars = listOf(RecordingPlugin(calls)),
            configure = { put(SETTING, 9) },
        )

        fixture.checkModule(module, File(directory, "explicit.txt"), mode = GoldenMode.UPDATE)

        assertEquals(listOf("<explicit>" to 9), calls)
    }

    @Test
    fun doesNotApplyClassDefaultsToExplicitDependencies() {
        val calls = mutableListOf<Pair<String, Int?>>()
        val dependency = PluginTestModule(
            "library",
            listOf(KotlinSource("Library.kt", "fun libraryValue() = 1")),
            registrars = listOf(RecordingPlugin(calls)),
        )
        val fixture = Fixture(registrars = listOf(RecordingPlugin(calls)))
        fixture.check(File(directory, "dependency.txt"), mode = GoldenMode.UPDATE, dependencies = listOf(dependency))
        assertEquals(listOf("<library>" to null, "<main>" to 7), calls)
    }

    @Test
    fun dependencyCanExplicitlyOptIntoClassDefaults() {
        val calls = mutableListOf<Pair<String, Int?>>()
        val fixture = Fixture(registrars = listOf(RecordingPlugin(calls)))
        val dependency = fixture.makeModule("library")
        fixture.check(File(directory, "dependency.txt"), mode = GoldenMode.UPDATE, dependencies = listOf(dependency))
        assertEquals(listOf("<library>" to 7, "<main>" to 7), calls)
    }

    @Test
    fun supportsClassAndPerTestStdlibSettings() {
        val fixture = Fixture(includeStdlib = false)
        assertFalse(fixture.makeModule().includeStdlib)
        val golden = File(directory, "stdlib.txt")
        assertThrows(AssertionError::class.java) { fixture.check(golden, mode = GoldenMode.UPDATE) }
        assertFalse(golden.exists())
        fixture.check(golden, mode = GoldenMode.UPDATE, includeStdlib = true)
        assertTrue(golden.readText().contains("listOf"))
    }

    @Test
    fun locatesJarAndDirectoryFromMarkerClasses() {
        val stdlib = classpathEntryFor<Unit>()
        assertTrue(stdlib.isFile)
        assertEquals(File(Unit::class.java.protectionDomain.codeSource.location.toURI()), stdlib)

        val classes = classpathEntryFor(AbstractCompilerPluginIrTestTest::class.java)
        assertTrue(classes.isDirectory)
        assertEquals(classpathEntryFor<AbstractCompilerPluginIrTestTest>(), classes)
    }

    @Test
    fun missingCodeSourceHasActionableError() {
        val failure = assertThrows(IllegalStateException::class.java) { classpathEntryFor<String>() }
        assertTrue(failure.message!!.contains("java.lang.String"))
        assertTrue(failure.message!!.contains("explicitly"))
    }

    private class Fixture(
        private val sources: List<KotlinSource> = listOf(KotlinSource("Test.kt", "fun values() = listOf(1, 2).size")),
        override val additionalClasspath: List<File> = emptyList(),
        override val registrars: List<CompilerPluginRegistrar> = emptyList(),
        override val includeStdlib: Boolean = true,
        override val renderer: IrRenderer = IrRenderer.KOTLIN_LIKE,
    ) : AbstractCompilerPluginIrTest() {
        override fun CompilerConfiguration.configureCompiler() {
            put(SETTING, 7)
        }

        fun makeModule(name: String = "main"): PluginTestModule = module(sources, name)

        fun checkModule(module: PluginTestModule, golden: File, mode: GoldenMode = GoldenMode.VERIFY) {
            verify(module, golden, mode)
        }

        fun check(
            golden: File,
            mode: GoldenMode = GoldenMode.VERIFY,
            dependencies: List<PluginTestModule> = emptyList(),
            extraClasspath: List<File> = emptyList(),
            includeStdlib: Boolean = this.includeStdlib,
            configure: CompilerConfiguration.() -> Unit = {},
        ) {
            verify(
                module(
                    sources = sources,
                    dependencies = dependencies,
                    additionalClasspath = extraClasspath,
                    includeStdlib = includeStdlib,
                    configure = configure,
                ),
                goldenFile = golden,
                mode = mode,
            )
        }
    }

    private class RecordingPlugin(private val calls: MutableList<Pair<String, Int?>>) : CompilerPluginRegistrar() {
        override val pluginId: String = "test.record.settings"
        override val supportsK2: Boolean = true

        override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
            val setting = configuration[SETTING]
            IrGenerationExtension.registerExtension(object : IrGenerationExtension {
                override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
                    calls += moduleFragment.name.asString() to setting
                }
            })
        }
    }

    companion object {
        private val SETTING = CompilerConfigurationKey.create<Int>("fixture setting")
    }
}
