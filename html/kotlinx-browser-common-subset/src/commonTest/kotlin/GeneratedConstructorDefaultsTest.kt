/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies constructor defaults on every target.
package kotlinx.browser.dom.constructors

import kotlinx.browser.dom.EventInit
import kotlinx.browser.dom.events.Event
import kotlin.test.Test

// A defaulted expect argument must resolve to the browser or JVM actual's default.
class GeneratedConstructorDefaultsTest {
    @Test
    fun defaultedConstructorArgumentIsCallable() {
        Event("common")
        Event("common", EventInit(bubbles = true))
    }
}
