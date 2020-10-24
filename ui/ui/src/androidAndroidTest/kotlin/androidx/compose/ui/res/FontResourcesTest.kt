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

package androidx.compose.ui.res

import androidx.compose.runtime.Providers
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.text.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.asFontFamily
import androidx.compose.ui.text.font.font
import androidx.compose.ui.text.font.fontFamily
import androidx.compose.ui.text.font.test.R
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@SmallTest
class FontResourcesTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun loadFontResource_systemFontFamily() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        var result: DeferredResource<Typeface>? = null
        var syncLoadedTypeface: Typeface? = null

        rule.setContent {
            Providers(ContextAmbient provides context) {

                // async API
                result = loadFontResource(
                    fontFamily = FontFamily.Monospace,
                    pendingFontFamily = FontFamily.Serif,
                    failedFontFamily = FontFamily.SansSerif
                )

                // sync API
                syncLoadedTypeface = fontResource(FontFamily.Monospace)
            }
        }

        rule.runOnIdle {
            assertThat(result).isNotNull()
            assertThat(result!!.state).isEqualTo(LoadingState.LOADED)
            assertThat(result!!.resource.resource).isEqualTo(
                syncLoadedTypeface
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadFontResource_systemFontFamily_FileListFamily_as_pendingFontFamily() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            Providers(ContextAmbient provides context) {
                loadFontResource(
                    fontFamily = font(R.font.sample_font).asFontFamily(),
                    pendingFontFamily = font(R.font.sample_font).asFontFamily(),
                    failedFontFamily = FontFamily.SansSerif
                )
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadFontResource_systemFontFamily_FileListFamily_as_failedFontFamily() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            Providers(ContextAmbient provides context) {
                loadFontResource(
                    fontFamily = font(R.font.sample_font).asFontFamily(),
                    pendingFontFamily = FontFamily.Serif,
                    failedFontFamily = font(R.font.sample_font).asFontFamily()
                )
            }
        }
    }

    @Test
    fun FontListFontFamily_cacheKey() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertThat(
            font(R.font.sample_font).asFontFamily().cacheKey(context)
        ).isEqualTo(
            font(R.font.sample_font).asFontFamily().cacheKey(context)
        )

        assertThat(
            font(R.font.sample_font).asFontFamily().cacheKey(context)
        ).isNotEqualTo(
            font(R.font.sample_font2).asFontFamily().cacheKey(context)
        )

        assertThat(
            fontFamily(
                font(R.font.sample_font, FontWeight.Normal),
                font(R.font.sample_font2, FontWeight.Bold)
            ).cacheKey(context)
        ).isNotEqualTo(
            font(R.font.sample_font).asFontFamily().cacheKey(context)
        )
    }
}