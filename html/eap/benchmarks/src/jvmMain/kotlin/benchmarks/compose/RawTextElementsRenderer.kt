/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.compose

import androidx.compose.runtime.Composable
import benchmarks.model.CatalogData
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Head
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.Script

// Keep the inputs stable so the benchmark measures rendering, not fixture construction.
private val rawTextStyleSheet = object : StyleSheet(usePrefix = false) {
    val benchmark by style {
        repeat(256) { index -> variable("item-$index", "${index}px") }
    }
}

private val styleHeavyCatalogSheet = object : StyleSheet(usePrefix = false) {
    val catalog by style {
        repeat(2048) { index -> variable("theme-token-$index", "${index}px") }
    }
}

// Repeated, balanced HTML comment markers exercise the script raw-text validator's
// search from each marker while remaining valid JavaScript and HTML raw text.
private val rawTextScript = InlineScript("const markers = `" + "<!-- -->".repeat(1000) + "`;")

@Composable
fun RawTextStyle() {
    Html {
        Head { Style(rawTextStyleSheet) }
        Body {}
    }
}

@Composable
fun StyleHeavyCatalog(data: CatalogData) {
    Html {
        Head { Style(styleHeavyCatalogSheet) }
        Body { TailwindCatalog(data) }
    }
}

@Composable
fun RawTextElements() {
    Html {
        Head {
            Style(rawTextStyleSheet)
            Script(rawTextScript)
        }
        Body {}
    }
}
