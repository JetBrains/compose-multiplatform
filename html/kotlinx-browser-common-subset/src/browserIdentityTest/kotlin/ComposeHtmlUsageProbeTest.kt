/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

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
