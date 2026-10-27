/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeDiagnosticsTest {
    @Test
    fun missingRuntimeRespectsExplicitOptInAndOptOut() {
        for (enabled in listOf(null, true, false)) {
            val (exit, output) = compile("fun answer() = 42", enabled)
            assertEquals(if (enabled == true) ExitCode.COMPILATION_ERROR else ExitCode.OK, exit, output)
            if (enabled == false) {
                assertFalse("[compose-html-lowering]" in output, output)
            } else {
                assertTrue((if (enabled == true) "error:" else "warning:") in output, output)
                assertTrue("org.jetbrains.compose.web.dom.WriteStaticHtml" in output, output)
            }
        }
    }

    @Test
    fun incompatibleRuntimeNamesTheMissingFunction() {
        val (exit, output) = compile(
            "package org.jetbrains.compose.web.dom\nfun UnrelatedRuntimeHelper() {}", enabled = true,
        )
        assertEquals(ExitCode.COMPILATION_ERROR, exit, output)
        assertTrue("org.jetbrains.compose.web.dom.WriteStaticHtml" in output, output)
    }

    @Test
    fun incompatibleRuntimeReportsAllMissingDeclarationsTogether() {
        val (exit, output) = compile("fun answer() = 42", enabled = true)
        assertEquals(ExitCode.COMPILATION_ERROR, exit, output)
        for (name in listOf(
            "WriteStaticHtml",
            "preparedHtmlAttributes",
            "HtmlStringWriterContext.writeText",
            "AttrsScope.style"
        )) {
            assertTrue(name in output, output)
        }
        assertEquals(1, Regex("Missing or ambiguous runtime declarations:").findAll(output).count(), output)
    }

    @Test
    fun incompatibleRuntimeNamesTheMissingAttributeHelper() {
        val (exit, output) = compile(
            """
                package org.jetbrains.compose.web.dom
                fun WriteStaticHtml(rootTag: String, encoded: String) {}
            """.trimIndent(),
            enabled = true,
        )
        assertEquals(ExitCode.COMPILATION_ERROR, exit, output)
        assertTrue("org.jetbrains.compose.web.dom.preparedHtmlAttributes" in output, output)
    }

    private fun compile(source: String, enabled: Boolean? = null): Pair<ExitCode, String> {
        val directory = Files.createTempDirectory("html-runtime-diagnostic").toFile()
        try {
            val input = directory.resolve("Sample.kt").apply { writeText(source) }
            return compileJvm(
                listOf(input), directory.resolve("classes"),
                java.io.File(Unit::class.java.protectionDomain.codeSource.location.toURI()).path,
                lowering = enabled,
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}
