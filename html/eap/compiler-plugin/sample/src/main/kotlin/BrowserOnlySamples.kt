/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun BrowserOnlyPage() {
    Span { Text("browser only") }
}

val storedBrowserContent: @Composable () -> Unit = { Span { Text("stored") } }

// Sharing a short name with the SSR entry point must not enable lowering.
private fun composeHtmlToString(content: @Composable () -> Unit): @Composable () -> Unit = content

val lookalikeContent = composeHtmlToString { Span { Text("lookalike") } }
