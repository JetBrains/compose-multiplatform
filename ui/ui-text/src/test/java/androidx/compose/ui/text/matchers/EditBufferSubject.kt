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

package androidx.compose.ui.text.matchers

import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.EditingBuffer
import androidx.compose.ui.text.input.PartialGapBuffer
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertAbout

@OptIn(InternalTextApi::class)
internal fun assertThat(buffer: PartialGapBuffer): EditBufferSubject {
    return assertAbout(EditBufferSubject.SUBJECT_FACTORY)
        .that(GapBufferWrapper(buffer))!!
}

internal fun assertThat(buffer: EditingBuffer): EditBufferSubject {
    return assertAbout(EditBufferSubject.SUBJECT_FACTORY)
        .that(EditingBufferWrapper(buffer))!!
}

internal abstract class GetOperatorWrapper(val buffer: Any) {
    abstract operator fun get(index: Int): Char
    override fun toString(): String = buffer.toString()
}

private class EditingBufferWrapper(buffer: EditingBuffer) : GetOperatorWrapper(buffer) {
    override fun get(index: Int): Char = (buffer as EditingBuffer)[index]
}

@OptIn(InternalTextApi::class)
private class GapBufferWrapper(buffer: PartialGapBuffer) : GetOperatorWrapper(buffer) {
    override fun get(index: Int): Char = (buffer as PartialGapBuffer)[index]
}

/**
 * Truth extension for Editing Buffers.
 */
@OptIn(InternalTextApi::class)
internal class EditBufferSubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: GetOperatorWrapper
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<EditBufferSubject, GetOperatorWrapper> =
            Factory { failureMetadata, subject -> EditBufferSubject(failureMetadata, subject) }
    }

    fun hasChars(expected: String) {
        assertThat(subject.buffer.toString()).isEqualTo(expected)
        for (i in expected.indices) {
            assertThat(subject[i]).isEqualTo(expected[i])
        }
    }
}