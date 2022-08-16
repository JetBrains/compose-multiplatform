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

package androidx.compose.foundation.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ValidatingOffsetMappingTest {

    private val invalidIndex = Int.MAX_VALUE
    private val text = "hello"

    @Test
    fun filterWithValidation_allowsZero_whenOriginalToTransformed() {
        val transformation = VisualTransformation { original ->
            TransformedText(original, object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = 0
                override fun transformedToOriginal(offset: Int): Int = throw NotImplementedError()
            })
        }

        val transformed = transformation.filterWithValidation(AnnotatedString(text))

        assertThat(transformed.offsetMapping.originalToTransformed(1)).isEqualTo(0)
    }

    @Test
    fun filterWithValidation_allowsZero_whenTransformedToOriginal() {
        val transformation = VisualTransformation { original ->
            TransformedText(original, object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = throw NotImplementedError()
                override fun transformedToOriginal(offset: Int): Int = 0
            })
        }

        val transformed = transformation.filterWithValidation(AnnotatedString(text))

        assertThat(transformed.offsetMapping.transformedToOriginal(1)).isEqualTo(0)
    }

    @Test
    fun filterWithValidation_allowsTextLength_whenOriginalToTransformed() {
        val transformation = VisualTransformation { original ->
            // Transformed text is longer to ensure transformed length is used for validation.
            TransformedText(original + original, object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = text.length * 2
                override fun transformedToOriginal(offset: Int): Int = throw NotImplementedError()
            })
        }

        val transformed = transformation.filterWithValidation(AnnotatedString(text))

        assertThat(transformed.offsetMapping.originalToTransformed(1)).isEqualTo(text.length * 2)
    }

    @Test
    fun filterWithValidation_allowsTextLength_whenTransformedToOriginal() {
        val transformation = VisualTransformation { original ->
            // Transformed text is shorter to ensure the original length is used for validation.
            TransformedText(original.subSequence(0, 1), object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = throw NotImplementedError()
                override fun transformedToOriginal(offset: Int): Int = text.length
            })
        }

        val transformed = transformation.filterWithValidation(AnnotatedString(text))

        assertThat(transformed.offsetMapping.transformedToOriginal(1)).isEqualTo(text.length)
    }

    @Test
    fun filterWithValidation_throws_whenInvalidOriginalToTransformed() {
        val transformation = VisualTransformation { original ->
            TransformedText(original, object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = invalidIndex
                override fun transformedToOriginal(offset: Int): Int = throw NotImplementedError()
            })
        }

        val transformed = transformation.filterWithValidation(AnnotatedString(text))

        val error = assertFailsWith<IllegalStateException> {
            transformed.offsetMapping.originalToTransformed(1)
        }

        assertThat(error).hasMessageThat().isEqualTo(
            "OffsetMapping.originalToTransformed returned invalid mapping: " +
                "1 -> $invalidIndex is not in range of transformed text [0, ${text.length}]"
        )
    }

    @Test
    fun filterWithValidation_throws_whenInvalidTransformedToOriginal() {
        val transformation = VisualTransformation { original ->
            TransformedText(original, object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = throw NotImplementedError()
                override fun transformedToOriginal(offset: Int): Int = invalidIndex
            })
        }

        val transformed = transformation.filterWithValidation(AnnotatedString(text))

        val error = assertFailsWith<IllegalStateException> {
            transformed.offsetMapping.transformedToOriginal(1)
        }

        assertThat(error).hasMessageThat().isEqualTo(
            "OffsetMapping.transformedToOriginal returned invalid mapping: " +
                "1 -> $invalidIndex is not in range of original text [0, ${text.length}]"
        )
    }
}