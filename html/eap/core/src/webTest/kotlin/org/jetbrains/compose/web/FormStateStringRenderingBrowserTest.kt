/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLTextAreaElement
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.internal.runtime.browserDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class FormStateStringRenderingBrowserTest {
    @Test
    fun clientRenderedTextareaDefaultsPreserveNewlinesAndUserEdits() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        val value = "first\nsecond & <third>"
        var default by mutableStateOf(value)
        val composition = renderComposable(root) { TextArea { defaultValue(default) } }
        try {
            val textarea = root.firstElementChild as HTMLTextAreaElement
            assertEquals(value, textarea.value)
            assertEquals(value, textarea.textContent)
            textarea.value = "typed"
            default = "new\ndefault"
            delay(100.milliseconds)
            assertEquals("typed", textarea.value)
            assertEquals("new\ndefault", textarea.textContent)
        } finally {
            composition.dispose()
        }
    }
}
