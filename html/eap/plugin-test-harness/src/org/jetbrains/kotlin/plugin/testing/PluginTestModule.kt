/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

/** A named Kotlin input. Names are relative paths and must be unique within a module. */
data class KotlinSource(val name: String, val text: String) {
    init {
        require(name.isNotBlank() && name.endsWith(".kt")) { "Expected a relative .kt source name: $name" }
        require(!File(name).isAbsolute && '\\' !in name && name.split('/').none { it == ".." || it == "." || it.isEmpty() }) {
            "Source name must be a normalized relative path: $name"
        }
    }

    companion object {
        /** Reads UTF-8 source, using [name] as its logical name in diagnostics and goldens. */
        fun fromFile(file: File, name: String = file.name): KotlinSource = KotlinSource(name, file.readText())
    }
}

/**
 * A single K2/JVM compilation. Dependencies are compiled to class directories before this module.
 * Plugins and configuration are per-module, not inherited from the consuming module.
 * All modules must use distinct names. The harness runtime's Kotlin standard library is included by default.
 * [classpath] supplies additional libraries; set [includeStdlib] to false to manage the standard library explicitly.
 */
class PluginTestModule(
    val name: String,
    val sources: List<KotlinSource>,
    val dependencies: List<PluginTestModule> = emptyList(),
    val classpath: List<File> = emptyList(),
    val registrars: List<CompilerPluginRegistrar> = emptyList(),
    val includeStdlib: Boolean = true,
    val configure: CompilerConfiguration.() -> Unit = {},
) {
    init {
        require(name.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid module name: $name" }
        require(sources.isNotEmpty()) { "Module $name has no sources" }
        require(sources.map { it.name }.distinct().size == sources.size) { "Duplicate source names in $name" }
        require(registrars.all { it.supportsK2 }) { "Only K2 compiler plugins are supported" }
        require(classpath.all { it.exists() }) { "Classpath entries must exist for module $name" }
    }
}
