/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.compose.html.compiler.lowering.FileLowering
import org.jetbrains.compose.html.compiler.lowering.WriterScopeSharing
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.platform.jvm.isJvm

internal const val HtmlCompilerPluginId = "org.jetbrains.compose.html.lowering"

@OptIn(ExperimentalCompilerApi::class)
class HtmlCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId = HtmlCompilerPluginId
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(
            HtmlIrGenerationExtension(configuration)
        )
    }
}

@OptIn(ExperimentalCompilerApi::class)
class HtmlCommandLineProcessor : CommandLineProcessor {
    override val pluginId = HtmlCompilerPluginId
    override val pluginOptions = listOf(
        CliOption("enabled", "true|false", "Enable static HTML lowering (defaults to JVM only)", required = false)
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "enabled" -> configuration.put(
                EnabledKey,
                value.toBooleanStrictOrNull()
                    ?: throw CliOptionProcessingException("Expected true or false for enabled, got '$value'")
            )

            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }

    companion object {
        internal val EnabledKey = CompilerConfigurationKey<Boolean>("Enable static HTML lowering")
    }
}

// Lowers HTML calls on JVM before the Compose compiler rewrites them.
@OptIn(MessageCollectorAccess::class)
internal class HtmlIrGenerationExtension(
    private val configuration: CompilerConfiguration
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val explicitEnabled = configuration.get(HtmlCommandLineProcessor.EnabledKey)
        val enabled = explicitEnabled ?: pluginContext.platform.isJvm()
        if (!enabled) return
        if (!pluginContext.platform.isJvm()) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "[compose-html-lowering] Currently only HTML string rendering on Kotlin/JVM is supported; " +
                        "skipping lowering for ${pluginContext.platform}."
            )
            return
        }

        val symbols = HtmlSymbols.resolve(pluginContext.finderForBuiltins()) { names ->
            configuration.messageCollector.report(
                if (explicitEnabled == true) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING,
                "[compose-html-lowering] Missing or ambiguous runtime declarations: ${names.joinToString()}. Skipping lowering. " +
                        "Check that the Compose HTML runtime and compiler plugin versions match. " +
                        "Set enabled=false to disable lowering for this compilation."
            )
        } ?: return
        // Replace HTML calls first, then reuse writer lookups in the generated code.
        for (file in moduleFragment.files) FileLowering(pluginContext, symbols, file).lower()
        moduleFragment.transformChildrenVoid(WriterScopeSharing(pluginContext, symbols))
    }
}
