/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun FontFamilies() {
    val state = rememberScrollState()
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .verticalScroll(state),
            verticalArrangement = Arrangement
                .spacedBy(10.dp)
        ) {
            for (fontFamily in listOf(
                FontFamily.SansSerif,
                FontFamily.Serif,
                FontFamily.Monospace,
                FontFamily.Cursive
            )) {
                FontFamilyShowcase(fontFamily)
            }
        }
    }
}

@Composable
fun FontFamilyShowcase(fontFamily: FontFamily) {
    Column {
        Text(
            text = "$fontFamily"
        )
        Text(
            text = "The quick brown fox jumps over the lazy dog.",
            fontSize = 48.sp,
            fontFamily = fontFamily
        )
        Text(
            text = "1234567890",
            fontSize = 48.sp,
            fontFamily = fontFamily
        )
    }
}
