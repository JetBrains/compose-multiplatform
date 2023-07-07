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
class LocaleListTest {

    @Test
    fun forLanguageTag_single() {
        val localeList = LocaleList("en-US")
        assertThat(localeList.size).isEqualTo(1)
        assertThat(localeList[0]).isEqualTo(Locale("en-US"))
    }

    @Test
    fun forLanguageTag_multiple() {
        val localeList = LocaleList("en-US,ja-JP")
        assertThat(localeList.size).isEqualTo(2)
        assertThat(localeList[0]).isEqualTo(Locale("en-US"))
        assertThat(localeList[1]).isEqualTo(Locale("ja-JP"))
    }

    @Test
    fun equals_order_matters() {
        assertThat(LocaleList("ja-JP,en-US"))
            .isNotEqualTo(LocaleList("en-US,ja-JP"))
    }

    @Test
    fun equals() {
        assertThat(LocaleList("en-US,ja-JP"))
            .isEqualTo(LocaleList("en-US,ja-JP"))
        assertThat(LocaleList("en-US,ja-JP"))
            .isNotEqualTo(LocaleList("en-US,es-ES"))
    }

    @Test
    fun getCurrent_afterJavaLocaleSetDefault() {
        val javaLocales = listOf(
            java.util.Locale("ar"),
            java.util.Locale("ja"),
            java.util.Locale("en")
        )
        for (javaLocale in javaLocales) {
            java.util.Locale.setDefault(javaLocale)

            assertThat(LocaleList.current.first()).isEqualTo(
                Locale(AndroidLocale(javaLocale))
            )
        }
    }
}