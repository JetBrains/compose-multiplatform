/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

/**
 * Optional, test-framework-independent convenience layer over [CompilerPluginTestHarness].
 * Defaults are read when a helper is called, not during construction.
 *
 * Only modules created with [module] receive these defaults. Dependency modules passed to
 * [module] or [verify] are used as-is; their plugins and configuration are never changed.
 */
abstract class AbstractCompilerPluginIrTest {
    /** Libraries added to the tested module's classpath, in addition to its automatic stdlib. */
    protected open val additionalClasspath: List<File> = emptyList()
    protected open val registrars: List<CompilerPluginRegistrar> = emptyList()
    protected open val includeStdlib: Boolean = true
    protected open val renderer: IrRenderer = IrRenderer.KOTLIN_LIKE

    /** Applied before the per-test configuration callback. */
    protected open fun CompilerConfiguration.configureCompiler() {}

    /**
     * Creates a module with this test class's defaults. Calling this explicitly for a dependency
     * opts that dependency into the same defaults. [additionalClasspath] augments the class defaults.
     */
    protected fun module(
        sources: List<KotlinSource>,
        name: String = "main",
        dependencies: List<PluginTestModule> = emptyList(),
        additionalClasspath: List<File> = emptyList(),
        includeStdlib: Boolean = this.includeStdlib,
        configure: CompilerConfiguration.() -> Unit = {},
    ): PluginTestModule = PluginTestModule(
        name = name,
        sources = sources,
        dependencies = dependencies,
        classpath = this.additionalClasspath + additionalClasspath,
        registrars = registrars,
        includeStdlib = includeStdlib,
        configure = {
            configureCompiler()
            configure()
        },
    )

    /** Compiles the tested module using class defaults and compares its IR with [goldenFile]. */
    protected fun verify(
        sources: List<KotlinSource>,
        goldenFile: File,
        name: String = "main",
        dependencies: List<PluginTestModule> = emptyList(),
        additionalClasspath: List<File> = emptyList(),
        includeStdlib: Boolean = this.includeStdlib,
        mode: GoldenMode = GoldenMode.VERIFY,
        configure: CompilerConfiguration.() -> Unit = {},
    ) {
        CompilerPluginTestHarness(renderer).verify(
            module(
                sources = sources,
                name = name,
                dependencies = dependencies,
                additionalClasspath = additionalClasspath,
                includeStdlib = includeStdlib,
                configure = configure,
            ),
            goldenFile,
            mode,
        )
    }
}
