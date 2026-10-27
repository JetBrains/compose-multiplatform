/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalWasmDsl::class)
class HtmlLoweringGradlePluginTest {
    private val project = ProjectBuilder.builder().build().also {
        it.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        it.pluginManager.apply("org.jetbrains.compose.html.lowering")
    }
    private val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
    private val plugin = project.plugins.getPlugin(HtmlLoweringGradlePlugin::class.java)
    private val extension = project.extensions.getByType(HtmlLoweringExtension::class.java)
    private val compilations = listOf(
        kotlin.jvm("server"),
        kotlin.jvm("otherServer"),
        kotlin.js("client").apply { browser() },
        kotlin.wasmJs("wasm").apply { browser() }
    ).flatMap { it.compilations.toList() }

    @Test
    fun onlyJvmTargetsAreEnabledByDefault() {
        assertEquals(setOf(KotlinPlatformType.jvm), extension.enabledTargets.get())
        for (compilation in compilations) {
            val isJvm = compilation.platformType == KotlinPlatformType.jvm
            assertEquals(isJvm, enabled(compilation))
            assertEquals(isJvm, ordered(compilation))
        }
    }

    @Test
    fun emptySetDisablesEveryTarget() {
        extension.enabledTargets.set(emptySet())
        for (compilation in compilations) {
            assertEquals(false, enabled(compilation))
            assertFalse(ordered(compilation))
        }
    }

    @Test
    fun platformSelectionReplacesDefaultsAndRemainsLazy() {
        val options = compilations.map { plugin.applyToCompilation(it) }
        compilations.forEach { it.compileTaskProvider.get() }
        extension.enabledTargets.set(setOf(KotlinPlatformType.js))
        for ((compilation, provider) in compilations.zip(options)) {
            assertEquals(
                (compilation.platformType == KotlinPlatformType.js).toString(),
                provider.get().single().value
            )
            assertFalse(ordered(compilation))
        }
        extension.enabledTargets.add(KotlinPlatformType.jvm)
        for ((compilation, provider) in compilations.zip(options)) {
            val isJvm = compilation.platformType == KotlinPlatformType.jvm
            assertEquals(
                (compilation.platformType != KotlinPlatformType.wasm).toString(),
                provider.get().single().value
            )
            assertEquals(isJvm, ordered(compilation))
        }
    }

    @Test
    fun everyPlatformCanBeSelected() {
        extension.enabledTargets.set(KotlinPlatformType.entries.toSet())
        for (compilation in compilations) {
            assertEquals(true, enabled(compilation))
            assertEquals(compilation.platformType == KotlinPlatformType.jvm, ordered(compilation))
        }
    }

    private fun ordered(compilation: KotlinCompilation<*>): Boolean =
        "-Xcompiler-plugin-order=org.jetbrains.compose.html.lowering>androidx.compose.compiler.plugins.kotlin" in
            compilation.compileTaskProvider.get().compilerOptions.freeCompilerArgs.get()

    private fun enabled(compilation: KotlinCompilation<*>): Boolean =
        plugin.applyToCompilation(compilation).get().single().value.toBooleanStrict()
}
