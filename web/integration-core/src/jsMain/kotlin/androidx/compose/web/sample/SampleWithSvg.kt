/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.sample

import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.SVGElement
import org.jetbrains.compose.web.dom.Svg
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.elements.SvgElementScope
import org.jetbrains.compose.web.renderComposableInBody

fun renderSampleWithSvg() {
    renderComposableInBody {
        Svg(
            viewBox = SvgElementScope.SvgViewBox(0, 0, 100, 100),
            attrs = {
                attr("width", "400px")
                attr("height", "400px")
            }
        ) {
            A(href = "http://${window.location.host}") {
                Circle(
                    cx = 10, cy = 10, r = 10
                ) {
                    attr("fill", "gray")
                }

                Text("Click me", x = 5, y = 10) {
                    // styleBuilder.color(Color.red) doesn't work, need to use `fill`
                    attr("fill", "red")
                    style {
                        fontSize(8.px)
                    }
                }
            }
        }
    }
}
