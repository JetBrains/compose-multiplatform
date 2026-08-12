// Runs the Compose-shaped probe against the JVM stubs.
package kotlinx.browser.dom.probe

import kotlinx.browser.dom.Document
import kotlin.test.Test

class ComposeHtmlUsageProbeJvmTest {
    @Test
    fun rendererOperationsAreCallable() {
        exerciseComposeHtmlUsage(Document())
    }
}
