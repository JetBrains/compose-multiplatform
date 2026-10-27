/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import java.io.File
import java.nio.file.Files

/** Called before lowering, while compiler resources are alive. Do not retain the supplied IR. */
fun interface IrRenderer {
    fun render(module: IrModuleFragment): String

    companion object {
        val KOTLIN_LIKE: IrRenderer = IrRenderer {
            it.dumpKotlinLike(KotlinLikeDumpOptions(printFilePath = false))
        }
    }
}

/**
 * Compiles dependency modules to JVM classes and the test module to post-plugin, pre-lowering IR.
 * The returned text, rather than a live compiler object, can safely outlive compilation.
 * Use the same compiler version for the harness and the plugin under test.
 */
class CompilerPluginTestHarness(private val renderer: IrRenderer = IrRenderer.KOTLIN_LIKE) {
    fun verify(module: PluginTestModule, goldenFile: File, mode: GoldenMode = GoldenMode.VERIFY) {
        GoldenFiles.assertMatches(goldenFile, render(module), mode)
    }

    /** Includes every module's inputs, but only the tested module's transformed IR. */
    fun render(module: PluginTestModule): String {
        val modules = dependencyOrder(module)
        val workspace = Files.createTempDirectory("kotlin-plugin-test-").toFile()
        try {
            val outputs = mutableMapOf<PluginTestModule, File>()
            var rendered: String? = null
            for (current in modules) {
                val directory = File(workspace, current.name).apply { mkdirs() }
                val sourceRoot = File(directory, "src")
                val sourceFiles = current.sources.map { source ->
                    File(sourceRoot, source.name).apply {
                        parentFile.mkdirs()
                        writeText(source.text)
                    }
                }
                val classpath = linkedSetOf<File>()
                val dependencies = mutableSetOf<PluginTestModule>()
                fun addDependencies(dependency: PluginTestModule) {
                    if (!dependencies.add(dependency)) return
                    classpath += outputs.getValue(dependency)
                    classpath += dependency.classpath
                    dependency.dependencies.forEach(::addDependencies)
                }
                current.dependencies.forEach(::addDependencies)
                classpath += current.classpath
                if (current.includeStdlib) classpath += defaultStdlib
                val output = File(directory, "classes").apply { mkdirs() }
                compileModule(
                    current, sourceFiles, classpath.toList(), output,
                    if (current === module) ({ ir -> rendered = renderer.render(ir) }) else null,
                )
                outputs[current] = output
            }
            return buildString {
                for (current in modules) {
                    for ((name, text) in current.sources) {
                        appendLine("// Source: ${current.name}/$name")
                        appendLine("// ------------------------------------------")
                        appendLine(text)
                        appendLine()
                    }
                }
                appendLine("// Transformed IR: ${module.name}")
                appendLine("// ------------------------------------------")
                append(checkNotNull(rendered) { "The compiler did not produce IR for ${module.name}" })
            }
        } finally {
            workspace.deleteRecursively()
        }
    }

    private val defaultStdlib: File by lazy { classpathEntryFor<Unit>() }

    private fun dependencyOrder(root: PluginTestModule): List<PluginTestModule> {
        val modules = mutableListOf<PluginTestModule>()
        val visiting = mutableSetOf<PluginTestModule>()
        val visited = mutableSetOf<PluginTestModule>()
        val names = mutableMapOf<String, PluginTestModule>()
        fun visit(module: PluginTestModule) {
            if (module in visited) return
            require(visiting.add(module)) { "Cyclic module dependency at ${module.name}" }
            val previous = names.put(module.name, module)
            require(previous == null || previous === module) { "Duplicate module name: ${module.name}" }
            module.dependencies.forEach(::visit)
            visiting.remove(module)
            visited.add(module)
            modules += module
        }
        visit(root)
        return modules
    }
}
