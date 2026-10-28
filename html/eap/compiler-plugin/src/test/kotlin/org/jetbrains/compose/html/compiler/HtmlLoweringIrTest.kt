/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import java.io.File
import kotlin.test.Test
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.plugin.testing.AbstractCompilerPluginIrTest
import org.jetbrains.kotlin.plugin.testing.KotlinSource
import org.jetbrains.kotlin.plugin.testing.PluginTestModule
import org.jetbrains.kotlin.plugin.testing.classpathEntryFor

/** Tests HTML lowering alone, before Compose adds composer parameters and groups. */
@OptIn(ExperimentalCompilerApi::class)
class HtmlLoweringIrTest : AbstractCompilerPluginIrTest() {
    override val registrars = listOf(HtmlCompilerPluginRegistrar())
    override val additionalClasspath = listOf(
        classpathEntryFor<Composable>(),
        classpathEntryFor<ElementScope<HTMLElement>>(),
        classpathEntryFor<DOMScope<HTMLElement>>(),
        classpathEntryFor<HTMLElement>(),
    )

    @Test
    fun staticHtml() = verifyFixture("staticHtml")

    @Test
    fun dynamicAttributesAndText() = verifyFixture("dynamicAttributesAndText")

    @Test
    fun controlFlow() = verifyFixture("controlFlow")

    @Test
    fun preparedAttributes() = verifyFixture("preparedAttributes")

    @Test
    fun unsupportedAttributes() = verifyFixture("unsupportedAttributes")

    @Test
    fun formControlPrecedence() = verifyFixture("formControlPrecedence")

    @Test
    fun callbackWriterScopes() = verifyFixture("callbackWriterScopes")

    @Test
    fun externalComposable() = verifyFixture(
        "externalComposable",
        dependencies = listOf(externalLibrary("Composable")),
    )

    @Test
    // Card is a <section> wrapper, compiled without the HTML plugin.
    // Its annotation lets us optimize calls and supported attrs without reading its body.
    // This is a promise from the external definition. The plugin cannot verify it.
    // Unsupported attrs keep the original call.
    fun externalWrapper() = verifyFixture(
        "externalWrapper",
        dependencies = listOf(externalLibrary("Wrapper")),
    )

    @Test
    fun disabledPlugin() = verifyFixture("staticHtml", golden = "staticHtml.disabled", enabled = false)

    private fun externalLibrary(name: String) = PluginTestModule(
        name = "external",
        sources = listOf(KotlinSource.fromFile(File("testData/ir/external/$name.kt"))),
        classpath = additionalClasspath,
        // Construct directly: dependencies must not inherit the consumer's HTML plugin.
        registrars = emptyList(),
        configure = { put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_11) },
    )

    private fun verifyFixture(
        name: String,
        golden: String = name,
        enabled: Boolean = true,
        dependencies: List<PluginTestModule> = emptyList(),
    ) {
        verify(
            module(
                sources = listOf(KotlinSource.fromFile(File("testData/ir/$name.kt"))),
                dependencies = dependencies,
                // Explicit enablement makes a runtime mismatch an error rather than silently skipping lowering.
                configure = {
                    put(HtmlCommandLineProcessor.EnabledKey, enabled)
                    // Match the runtime target for inline helpers such as CompositionLocal.provides.
                    put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_11)
                },
            ),
            goldenFile = File("testData/ir/$golden.txt"),
        )
    }
}
