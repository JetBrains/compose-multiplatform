/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import java.io.File

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

fun runJavaTool(toolName: String, vararg args: String): ProcessRunResult {
    val javaHome = File(System.getProperty("java.home"))
    val toolExecutableName = if (isWindows) "$toolName.exe" else toolName
    val executable = javaHome.resolve("bin/$toolExecutableName")
    check(executable.isFile) { "Could not find tool '$toolName' at specified path: $executable" }
    return runProcess(executable, args.toList())
}