/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun KeyboardTypeDemo() {
    LazyColumn {
        item { Item(KeyboardType.Text) }
        item { Item(KeyboardType.Ascii) }
        item { Item(KeyboardType.Number) }
        item { Item(KeyboardType.Phone) }
        item { Item(KeyboardType.Uri) }
        item { Item(KeyboardType.Email) }
        item { Item(KeyboardType.Password) }
        item { Item(KeyboardType.NumberPassword) }
    }
}

@Composable
private fun Item(keyboardType: KeyboardType) {
    TagLine(tag = "Keyboard Type: $keyboardType")
    EditLine(keyboardType = keyboardType)
}
