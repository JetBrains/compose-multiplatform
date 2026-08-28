package org.jetbrains.compose.web

import java.io.File

internal object SsrHydrationFixtureGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        val outputFile = File(requireNotNull(args.singleOrNull()) {
            "Expected the output fixture path"
        })
        val serverRenderedContent = composeHtmlToString {
            SsrHydrationContent(count = 0, increment = {})
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText("\n    $serverRenderedContent\n")
    }
}
