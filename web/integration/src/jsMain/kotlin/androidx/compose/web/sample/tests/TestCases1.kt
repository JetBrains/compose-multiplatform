/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.css.backgroundColor
import androidx.compose.web.css.height
import androidx.compose.web.css.px
import androidx.compose.web.css.width
import androidx.compose.web.elements.Button
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.Text

class TestCases1 {
    val helloWorldText by testCase {
        Div { Text("Hello World!") }
    }

    val buttonClicksUpdateCounterValue by testCase {
        var count by remember { mutableStateOf(0) }

        TestText(value = count.toString())

        Button(
            attrs = {
                id("btn")
                onClick { count += 1 }
            }
        ) {
            Text("Button")
        }
    }

    val hoverOnDivUpdatesText by testCase {
        var hovered by remember { mutableStateOf(false) }

        TestText(
            value = if (hovered) "hovered" else "not hovered"
        )

        Div(
            attrs = {
                id("box")
                onMouseEnter {
                    println("Mouse enter")
                    hovered = true
                }
                onMouseLeave {
                    println("Mouse leave")
                    hovered = false
                }
            },
            style = {
                width(100.px)
                height(100.px)
                backgroundColor("red")
            }
        ) {}
    }
}