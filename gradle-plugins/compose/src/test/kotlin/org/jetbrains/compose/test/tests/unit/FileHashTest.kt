/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.unit

import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.desktop.application.internal.files.contentHash

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

class FileHashTest {
    @TempDir
    lateinit var workingDir: File

    private val inputDir: File
        get() = workingDir.resolve("inputs").apply { mkdirs() }

    private fun inputFile(name: String, content: String): File {
        return inputDir.resolve(name).apply { writeText(content) }
    }

    private fun outputFile(name: String) =
        workingDir.resolve("outputs/$name").apply {
            parentFile.mkdirs()
        }

    @Test
    fun testFileHashIsAffectedByContent() {
        val input1 = inputFile("input1.txt", "1")

        val initJar =  createJar("init", input1)
        input1.writeText("2")
        val modifiedJar = createJar("modified", input1)

        val initHash = initJar.contentHash()
        val modifiedHash = modifiedJar.contentHash()
        assertNotEquals(modifiedHash, initHash)
    }

    private fun createJar(outputFileName: String, vararg files: File): File {
        val outputFile = outputFile(outputFileName)

        val cmd = arrayListOf(jarUtilFile.absolutePath, "cvf", outputFile.absolutePath)
        for (file in files) {
            cmd.add(file.relativeTo(inputDir).path)
        }
        val outFile = workingDir.resolve("jar-stdout.txt").apply { delete() }
        val errFile = workingDir.resolve("jar-error.txt").apply { delete() }
        val process = ProcessBuilder(cmd).run {
            redirectOutput(outFile)
            redirectError(errFile)
            directory(inputDir)
            start()
        }
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            error("Process hang up: [${cmd.joinToString(" ")}]")
        }
        val exitCode = process.exitValue()
        check(exitCode == 0) {
            """
                Stdout log: $outFile
                Error log: $errFile
                Process exited with error: $exitCode
            """
        }
        outFile.delete()
        errFile.delete()

        return outputFile
    }
}

private val jarUtilFile = run {
    val javaHome = File(System.getProperty("java.home"))
    val executableName = if (currentOS == OS.Windows) "jar.exe" else "jar"
    javaHome.resolve("bin/$executableName")
}