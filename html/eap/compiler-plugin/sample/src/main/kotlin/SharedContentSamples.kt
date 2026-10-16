/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

val sharedContent: @Composable () -> Unit = { Span { Text("shared lambda") } }

@Composable
fun SharedPage() {
    Span { Text("shared function") }
}

@Composable
fun UnusedSharedPage() {
    Span { Text("unused") }
}
