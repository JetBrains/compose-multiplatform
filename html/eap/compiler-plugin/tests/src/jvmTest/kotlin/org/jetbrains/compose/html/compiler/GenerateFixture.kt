/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.compose.html.compiler.samples.LargeStaticHydration
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.composeHtmlToStream
import java.io.File

fun main(args: Array<String>) {
    val target = File(args.single(), "CompilerHydrationFixture.kt").apply { parentFile.mkdirs() }
    target.writeText(buildString {
        appendLine("package org.jetbrains.compose.html.compiler")
        val html = composeHtmlToString { CompilerHydrationContent(0) {} }
        appendLine("val jvmHydrationHtml = ${html.kotlinLiteral()}")
        val scoped = composeHtmlToString { ScopedHydrationContent(0, {}) }
        appendLine("val jvmScopedHydrationHtml = ${scoped.kotlinLiteral()}")
        val large = StringBuilder()
        composeHtmlToStream(chunkSize = 1, sink = { large.append(it) }) { LargeStaticHydration(0, {}) }
        appendLine("val jvmLargeHydrationHtml = ${large.toString().kotlinLiteral()}")
    })
}

private fun String.kotlinLiteral(): String = "\"" + replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("$", "\\$")
    .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\""
