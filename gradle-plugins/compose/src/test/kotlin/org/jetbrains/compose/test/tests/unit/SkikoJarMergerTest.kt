/* Copyright 2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license. */
package org.jetbrains.compose.test.tests.unit

import org.jetbrains.compose.desktop.application.internal.mergeSkikoJars
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SkikoJarMergerTest {
    @TempDir lateinit var dir: File

    private fun jar(name: String, vararg entries: Pair<String, String>): File = dir.resolve(name).also { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((path, content) in entries) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    @Test fun mergesServicesPreservesNativesAndProducesReproducibleOutput() {
        val first = jar("core.jar", "META-INF/services/example.Service" to "example.A\n",
            "META-INF/CORE.SF" to "stale signature",
            "org/jetbrains/skiko/Core.class" to "core")
        val second = jar("backend.jar", "META-INF/services/example.Service" to "# comment\nexample.A\nexample.B\n",
            "libskiko-macos-arm64.dylib" to "signed native bytes",
            "libskiko-macos-arm64.dylib.sha256" to "checksum")
        val output = dir.resolve("merged.jar")
        val reordered = dir.resolve("reordered.jar")
        mergeSkikoJars(listOf(first, second), output)
        mergeSkikoJars(listOf(second, first), reordered)
        assertArrayEquals(output.readBytes(), reordered.readBytes())
        JarFile(output).use { zip ->
            fun text(name: String) = zip.getInputStream(zip.getJarEntry(name)).bufferedReader().readText()
            assertEquals("example.A\nexample.B\n", text("META-INF/services/example.Service"))
            assertEquals("signed native bytes", text("libskiko-macos-arm64.dylib"))
            assertEquals("checksum", text("libskiko-macos-arm64.dylib.sha256"))
            assertNull(zip.getJarEntry("META-INF/CORE.SF"))
        }
    }

    @Test fun rejectsConflictingClassesInsteadOfSilentlyChoosingOne() {
        val first = jar("core.jar", "org/jetbrains/skiko/Library.class" to "first")
        val second = jar("backend.jar", "org/jetbrains/skiko/Library.class" to "different")
        val error = assertThrows(IllegalStateException::class.java) {
            mergeSkikoJars(listOf(first, second), dir.resolve("merged.jar"))
        }
        assertTrue(error.message!!.contains("org/jetbrains/skiko/Library.class"))
    }
}
