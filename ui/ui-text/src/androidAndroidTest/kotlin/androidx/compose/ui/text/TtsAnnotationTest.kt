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

package androidx.compose.ui.text

import android.text.style.TtsSpan
import androidx.compose.ui.text.platform.extensions.toSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TtsAnnotationTest {
    @Test
    fun VerbatimTtsAnnotation() {
        val verbatim = "verbatim"
        val ttsAnnotation = VerbatimTtsAnnotation(verbatim)
        val ttsSpan = ttsAnnotation.toSpan()

        assertThat(ttsSpan.type).isEqualTo(TtsSpan.TYPE_VERBATIM)
        assertThat(ttsSpan.args.size()).isEqualTo(1)
        assertThat(ttsSpan.args.getString(TtsSpan.ARG_VERBATIM)).isEqualTo(verbatim)
    }
}