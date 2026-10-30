/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.hydration

import java.io.File
import org.jetbrains.compose.web.composeHtmlToString

fun main(args: Array<String>) {
    val source = File(args.single(), "HydrationFixtures.kt")
    source.parentFile.mkdirs()
    source.writeText(buildString {
        appendLine("// Generated from the JVM renderer; do not edit.")
        appendLine("package benchmarks.hydration")
        appendLine("internal val hydrationHtml = mapOf(")
        for (workload in hydrationWorkloads) {
            val html = composeHtmlToString(content = hydrationContent(workload))
            appendLine("    ${workload.quoted()} to ${html.quoted()},")
        }
        appendLine(")")
    })
}

private fun String.quoted(): String = "\"" + replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("$", "\\$")
    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
