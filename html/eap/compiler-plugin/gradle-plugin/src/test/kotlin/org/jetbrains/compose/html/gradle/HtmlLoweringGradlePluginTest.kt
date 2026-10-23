/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
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
        kotlin.js("client").apply { browser() },
        kotlin.wasmJs("wasm").apply { browser() }
    ).flatMap { it.compilations.toList() }

    @Test
    fun globalOverrideCanEnableOrDisableEveryTarget() {
        extension.enabled.set(true)
        for (compilation in compilations) {
            assertEquals(true, enabled(compilation))
            assertEquals(compilation.target.name == "server", ordered(compilation))
        }
        extension.enabled.set(false)
        for (compilation in compilations) {
            assertEquals(false, enabled(compilation))
            assertFalse(ordered(compilation))
        }
    }

    @Test
    fun namedTargetsOverrideGlobalSettingAndRemainLazy() {
        val options = compilations.map { plugin.applyToCompilation(it) }
        extension.enabled.set(false)
        extension.targets.put("client", true)
        for ((compilation, provider) in compilations.zip(options)) {
            assertEquals(
                (compilation.target.name == "client").toString(),
                provider.get().single().value
            )
            assertFalse(ordered(compilation))
        }
        extension.enabled.set(true)
        extension.targets.put("server", false)
        for (compilation in compilations) {
            assertEquals(compilation.target.name != "server", enabled(compilation))
            assertFalse(ordered(compilation))
        }
    }

    private fun ordered(compilation: KotlinCompilation<*>): Boolean =
        "-Xcompiler-plugin-order=org.jetbrains.compose.html.lowering>androidx.compose.compiler.plugins.kotlin" in
            compilation.compileTaskProvider.get().compilerOptions.freeCompilerArgs.get()

    private fun enabled(compilation: KotlinCompilation<*>): Boolean =
        plugin.applyToCompilation(compilation).get().singleOrNull()?.value?.toBooleanStrict()
            ?: (compilation.target.name == "server")
}
