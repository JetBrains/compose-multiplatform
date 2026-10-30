/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.compose

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun FractionalStyle() {
    Div {
        repeat(256) { index ->
            // These fractions serialize identically from Kotlin Float and JavaScript Number.
            val number = (index + 1) / 8f
            val overwrite = index % 2 == 1
            Div({
                attr("data-testid", "fractional-style")
                style {
                    width(number.px)
                    height((number / 4f).em)
                    property("opacity", (index % 31 + 1) / 32f)
                    property("line-height", 1.375f + number / 8f, overwrite)
                    variable("progress", number / 8f)
                    if (overwrite) {
                        property("line-height", 1.625f + number / 8f)
                        property("margin-left", "1px")
                        property("margin-left", "")
                    }
                }
            }) { Text("$index") }
        }
    }
}
