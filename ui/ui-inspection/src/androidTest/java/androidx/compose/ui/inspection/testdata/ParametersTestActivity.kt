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

package androidx.compose.ui.inspection.testdata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.inspection.test.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

class ParametersTestActivity : ComponentActivity() {
    private val fontFamily = FontFamily(
        Font(
            resId = R.font.samplefont,
            weight = FontWeight.W400,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.samplefont,
            weight = FontWeight.W400,
            style = FontStyle.Italic
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(modifier = Modifier.semantics(true) {}) {
                Button(onClick = { println("smth") }) {
                    Text("one", fontFamily = fontFamily)
                }
                Button(onClick = ::testClickHandler) {
                    Text("two", fontFamily = fontFamily)
                }
                FunctionWithIntArray(intArrayOf(10, 11, 12, 13, 14, 15, 16, 17))
                Text("four")
                SomeContent {
                    Column {
                        Text("five")
                    }
                }
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun FunctionWithIntArray(intArray: IntArray) {
    Text("three")
}

@Composable
fun SomeContent(content: @Composable () -> Unit) = content()

internal fun testClickHandler() {}