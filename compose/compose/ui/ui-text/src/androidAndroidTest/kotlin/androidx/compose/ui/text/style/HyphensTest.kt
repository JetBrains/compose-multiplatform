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

package androidx.compose.ui.text.style

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalTextApi::class)
@SdkSuppress(minSdkVersion = 28)
class HyphensTest : TextLineBreaker() {
    private val text = "Transformation"

    @Test
    fun check_hyphens_Auto() {
        val brokenLines = breakTextIntoLines(
            text = text,
            hyphens = Hyphens.Auto,
            maxWidth = 30
        )
        val expected = listOf(
            "Tran",
            "sfor",
            "ma",
            "tion"
        )
        assertThat(brokenLines).isEqualTo(expected)
    }

    @Test
    fun check_hyphens_None() {
        val brokenLines = breakTextIntoLines(
            text = text,
            hyphens = Hyphens.None,
            maxWidth = 30
        )
        val expected = listOf(
            "Tran",
            "sfor",
            "mati",
            "on"
        )
        assertThat(brokenLines).isEqualTo(expected)
    }
}