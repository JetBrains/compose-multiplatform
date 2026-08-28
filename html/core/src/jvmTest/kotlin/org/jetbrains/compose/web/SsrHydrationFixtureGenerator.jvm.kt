package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import java.io.File

internal object SsrHydrationFixtureGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        val outputDirectory = File(requireNotNull(args.singleOrNull()) {
            "Expected the output fixture directory"
        })

        outputDirectory.mkdirs()
        outputDirectory.writeFixture("ssr-hydration.html") {
            SsrHydrationContent(count = 0, increment = {})
        }
        outputDirectory.writeFixture("ssr-number-hydration.html") {
            SsrNumberHydrationContent(count = 0, increment = {})
        }
    }

    private fun File.writeFixture(name: String, content: @Composable () -> Unit) {
        resolve(name).writeText("\n    ${composeHtmlToString(content)}\n")
    }
}
