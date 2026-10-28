/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import external.CARD_CLASS
import external.Card
import org.jetbrains.compose.web.dom.Text

// The annotation says Card forwards attrs to <section>. We don't inspect its body.
// Unsupported attrs keep the Card call.
@Composable
fun UseCard(value: String) {
    Card(attrs = { classes(CARD_CLASS) }) { Text("Hello<&") }
    Card { Text(value) }
}
