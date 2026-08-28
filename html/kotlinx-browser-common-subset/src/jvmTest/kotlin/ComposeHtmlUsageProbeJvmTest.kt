/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

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
