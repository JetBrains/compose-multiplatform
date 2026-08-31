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
        outputDirectory.writeHydrationDataFixture("ssr-hydration-data.html")
    }

    private fun File.writeFixture(name: String, content: @Composable () -> Unit) {
        resolve(name).writeText("\n    ${composeHtmlToString(content = content)}\n")
    }

    private fun File.writeHydrationDataFixture(name: String) {
        val data = SsrHydrationData(
            label = "Loaded by JVM <backend>",
            count = 41,
        )
        val rendered = composeHtmlToString(
            data = data,
            serializeData = SsrHydrationData::toJson,
        ) { initialData ->
            SsrHydrationDataContent(
                label = initialData.label,
                count = initialData.count,
                increment = {},
            )
        }

        resolve(name).writeText(
            "\n    <div id=\"$SSR_HYDRATION_DATA_ROOT_ID\">${rendered.content}</div>\n" +
                "    ${rendered.hydrationDataElement}\n",
        )
    }
}
