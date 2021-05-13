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

package androidx.compose.ui.text.input

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PasswordVisualTransformationTest {
    @Test
    fun check_visual_output_is_masked_with_asterisk() {
        val transformation = PasswordVisualTransformation(mask = '*')
        val text = AnnotatedString("12345")
        val transformedText = transformation.filter(text)
        val visualText = transformedText.text
        val offsetMapping = transformedText.offsetMapping

        assertThat(visualText.text).isEqualTo("*****")
        for (i in 0..visualText.text.length) {
            assertThat(offsetMapping.originalToTransformed(i)).isEqualTo(i)
            assertThat(offsetMapping.transformedToOriginal(i)).isEqualTo(i)
        }
    }

    @Test
    fun check_visual_output_is_masked_with_default() {
        val filter = PasswordVisualTransformation()
        val text = AnnotatedString("1234567890")
        val transformedText = filter.filter(text)
        val visualText = transformedText.text
        val offsetMapping = transformedText.offsetMapping

        assertThat(visualText.text).isEqualTo("\u2022".repeat(10))
        for (i in 0..visualText.text.length) {
            assertThat(offsetMapping.originalToTransformed(i)).isEqualTo(i)
            assertThat(offsetMapping.transformedToOriginal(i)).isEqualTo(i)
        }
    }
}