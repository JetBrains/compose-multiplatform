/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendFilesForPluginsGenerationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File

internal fun compileModule(
    module: PluginTestModule,
    sources: List<File>,
    classpath: List<File>,
    output: File,
    capture: ((IrModuleFragment) -> Unit)?,
) {
    val arguments = K2JVMCompilerArguments().apply {
        freeArgs = sources.map { it.absolutePath }
        destination = output.absolutePath
        moduleName = module.name
        this.classpath = classpath.joinToString(File.pathSeparator) { it.absolutePath }
        noStdlib = true
        noReflect = true
        disableDefaultScriptingPlugin = true
        jdkHome = System.getProperty("java.home")
        jvmTarget = "1.8"
    }
    val messages = CompilationMessages()
    val result = TestPipeline(module, capture).execute(arguments, Services.EMPTY, messages)
    if (result != ExitCode.OK || messages.hasErrors()) {
        throw AssertionError("Compilation of module '${module.name}' failed ($result):\n${messages.text}")
    }
}

private class TestPipeline(
    private val module: PluginTestModule,
    private val capture: ((IrModuleFragment) -> Unit)?,
) : AbstractCliPipeline<K2JVMCompilerArguments>() {
    override val defaultPerformanceManager: PerformanceManager =
        object : PerformanceManager(JvmPlatforms.defaultJvmPlatform, "Plugin test harness") {}

    override fun createCompoundPhase(
        arguments: K2JVMCompilerArguments,
    ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, *> {
        val configure = object : PipelinePhase<ConfigurationPipelineArtifact, ConfigurationPipelineArtifact>("ConfigureTestPlugin") {
            override fun executePhase(input: ConfigurationPipelineArtifact): ConfigurationPipelineArtifact {
                input.configuration.apply {
                    module.configure(this)
                    require(languageVersionSettings.languageVersion.usesK2) { "The harness only supports K2" }
                    module.registrars.forEach { add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, it) }
                }
                return input
            }
        }
        val frontend = JvmConfigurationPipelinePhase then configure then
                JvmFrontendPipelinePhase then FrontendFilesForPluginsGenerationPipelinePhase() then JvmFir2IrPipelinePhase

        if (capture != null) {
            val render = object : PipelinePhase<JvmFir2IrPipelineArtifact, JvmFir2IrPipelineArtifact>("CaptureTestIr") {
                override fun executePhase(input: JvmFir2IrPipelineArtifact): JvmFir2IrPipelineArtifact {
                    capture.invoke(input.result.irModuleFragment)
                    return input
                }
            }
            return frontend then render
        }
        return frontend then JvmBackendPipelinePhase then JvmWriteOutputsPhase
    }
}

private class CompilationMessages : MessageCollector {
    private val messages = StringBuilder()
    private var errors = false
    val text: String get() = messages.toString()

    override fun clear() {
        messages.setLength(0)
        errors = false
    }

    override fun hasErrors(): Boolean = errors

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        errors = errors || severity.isError
        if (severity == CompilerMessageSeverity.LOGGING) return
        messages.append(severity).append(": ")
        if (location != null) messages.append("${location.path}:${location.line}:${location.column}: ")
        messages.appendLine(message)
    }
}
