/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import java.io.File
import kotlin.io.path.isExecutable

const val JDK_11_BYTECODE_VERSION = 55

fun readClassFileVersion(classFile: File): Int {
    val url = classFile.toURI().toURL().toExternalForm()
    val javapResult = runJavaTool("javap", "-verbose", url)
    val majorVersionRegex = "major version: (\\d+)".toRegex()
    val bytecode = javapResult.out
    val match = majorVersionRegex.find(bytecode)
        ?: error(buildString {
            appendLine("Could not find 'major version' in '$classFile' bytecode:")
            appendLine(bytecode)
        })
    return match.groupValues[1].toInt()
}

internal fun javaToolExecutableName(name: String): String =
    if (isWindows) "$name.exe" else name

internal fun runJavaTool(toolName: String, vararg args: String): ProcessRunResult {
    val javaHome = File(System.getProperty("java.home"))
    val executable = javaHome.resolve("bin/${javaToolExecutableName(toolName)}")
    check(executable.isFile) { "Could not find tool '$toolName' at specified path: $executable" }
    return runProcess(executable, args.toList())
}

/**
 * Expects the following structure:
 * [rootDir]
 * -- JDK_VERSION_1
 *    -- UNPACKED_JDK_1_DISTRIBUTION_ARCHIVE
 * -- JDK_VERSION_2
 *    -- UNPACKED_JDK_2_DISTRIBUTION_ARCHIVE
 *
 * where JDK_VERSION_* is an integer corresponding to the major version of JDK distribution
 */
internal fun listTestJdks(rootDir: File): List<String> {
    if (!rootDir.isDirectory) return emptyList()

    return rootDir.listFiles()!!
        .filter { it.isDirectory }
        .map { findJavaHome(it).absolutePath }
}

private fun findJavaHome(dir: File): File {
    val javaExecutableName = javaToolExecutableName("java")
    val javaExecutable = dir.walk()
        .firstOrNull { it.isFile && it.name == javaExecutableName && it.toPath().isExecutable() }
        ?: error("Could not find executable '$javaExecutableName' in '$dir' directory")
    return javaExecutable.parentFile.parentFile.absoluteFile
}