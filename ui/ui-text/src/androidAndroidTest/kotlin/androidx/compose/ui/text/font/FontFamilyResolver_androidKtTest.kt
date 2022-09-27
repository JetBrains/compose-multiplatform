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

package androidx.compose.ui.text.font

import android.graphics.Typeface.SANS_SERIF
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class FontFamilyResolver_androidKtTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun resolveAsTypeface_producesTypeface() {
        val loader = AsyncTestTypefaceLoader()
        val font = BlockingFauxFont(loader, SANS_SERIF)
        val subject = createFontFamilyResolver(context)
        val result = subject.resolveAsTypeface(font.toFontFamily())
        assertThat(result.value).isSameInstanceAs(SANS_SERIF)
    }
}