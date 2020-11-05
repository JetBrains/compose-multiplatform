/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.intl

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocaleTest {

    @Test
    fun forLanguageTag() {
        val locale = Locale("en-US")
        assertThat(locale.language).isEqualTo("en")
        assertThat(locale.script).isEmpty()
        assertThat(locale.region).isEqualTo("US")
    }

    @Test
    fun forLanguageTag_with_script() {
        val locale = Locale("sr-Latn-SR")
        assertThat(locale.language).isEqualTo("sr")
        assertThat(locale.script).isEqualTo("Latn")
        assertThat(locale.region).isEqualTo("SR")
    }

    @Test
    fun equals_language() {
        assertThat(Locale("ja")).isEqualTo(Locale("ja"))
        assertThat(Locale("ja")).isNotEqualTo(Locale("en"))
    }

    @Test
    fun equals_language_region() {
        assertThat(Locale("en-US")).isEqualTo(Locale("en-US"))
        assertThat(Locale("en-US")).isNotEqualTo(Locale("en-GB"))
    }

    @Test
    fun equals_script() {
        assertThat(Locale("sr-Latn-SR")).isEqualTo(Locale("sr-Latn-SR"))
        assertThat(Locale("sr-Latn-SR")).isNotEqualTo(Locale("sr-Cyrl-SR"))
    }
}