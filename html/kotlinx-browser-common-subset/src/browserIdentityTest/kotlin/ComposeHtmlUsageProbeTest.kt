// Runs the Compose-shaped probe against the browser DOM.
package kotlinx.browser.dom.probe

import kotlinx.browser.document
import kotlin.test.Test

class ComposeHtmlUsageProbeTest {
    @Test
    fun rendererOperationsAreCallable() {
        exerciseComposeHtmlUsage(document)
    }
}
