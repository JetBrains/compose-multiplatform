package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import java.io.File
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Html

internal object SsrHydrationFixtureGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        val outputDirectory = File(requireNotNull(args.singleOrNull()) {
            "Expected the output fixture directory"
        })

        outputDirectory.mkdirs()
        outputDirectory.writeFixture("ssr-hydration.html") {
            SsrHydrationContent(
                count = 0,
                renderedAt = SSR_HYDRATION_SERVER_RENDERED_AT,
                increment = {},
            )
        }
        outputDirectory.writeFixture("ssr-number-hydration.html") {
            SsrNumberHydrationContent(count = 0, increment = {})
        }
        outputDirectory.writeHydrationStateFixture("ssr-hydration-state.html")
        outputDirectory.writeHydrationIslandsFixture("ssr-hydration-islands.html")
        outputDirectory.writeHydrationStateFixture("ssr-hydration-state-stream-fast.html", streamedStrictMode = false)
        outputDirectory.writeHydrationStateFixture("ssr-hydration-state-stream-strict.html", streamedStrictMode = true)
    }

    private fun File.writeFixture(name: String, content: @Composable () -> Unit) {
        resolve(name).writeText("\n    ${composeHtmlToString(content = content)}\n")
    }

    private fun File.writeHydrationStateFixture(name: String, streamedStrictMode: Boolean? = null) {
        val state = SsrHydrationState(
            label = "Loaded by JVM <backend>",
            count = 41,
        )
        val content: @Composable () -> Unit = {
            Html {
                Body {
                    HydrationRoot(
                        initialState = state,
                        serializeState = SsrHydrationState::toJson,
                        content = { initialState ->
                            SsrHydrationStateApplication(initialState)
                        },
                    )
                }
            }
        }

        if (streamedStrictMode == null) {
            resolve(name).writeText(renderHydratedDocument(content = content))
        } else {
            resolve(name).bufferedWriter().use { writer ->
                renderHydratedDocumentToStream(writer, chunkSize = 1, validateStrictly = streamedStrictMode, content = content)
            }
        }
    }

    private fun File.writeHydrationIslandsFixture(name: String) {
        val cartState = SsrHydrationState(label = "Cart loaded by JVM", count = 41)
        val accountState = SsrHydrationState(label = "Account loaded by JVM", count = 7)
        val rendered = renderHydratedDocument {
            SsrHydrationIslandsDocument(cartState, accountState)
        }

        resolve(name).writeText(rendered)
    }
}
