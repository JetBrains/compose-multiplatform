/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun EmojiCompatDemo() {
    val emoji14MeltingFace = "\uD83E\uDEE0"
    val emoji13WomanFeedingBaby = "\uD83D\uDC69\uD83C\uDFFE\u200D\uD83C\uDF7C"
    val emoji13DisguisedFace = "\uD83E\uDD78"
    val emoji12HoldingHands =
        "\uD83D\uDC69\uD83C\uDFFB\u200D\uD83E\uDD1D\u200D\uD83D\uDC68\uD83C\uDFFF"
    val emoji12Flamingo = "\uD83E\uDDA9"
    val emoji11PartyingFace = "\uD83E\uDD73"

    val text = "11: $emoji11PartyingFace 12: $emoji12Flamingo $emoji12HoldingHands " +
        "13: $emoji13DisguisedFace $emoji13WomanFeedingBaby " +
        "14: $emoji14MeltingFace"

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(text = text, modifier = Modifier.padding(16.dp))

        val textFieldValue =
            rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(text))
            }

        TextField(
            value = textFieldValue.value,
            modifier = Modifier.padding(16.dp),
            onValueChange = { textFieldValue.value = it }
        )
    }
}