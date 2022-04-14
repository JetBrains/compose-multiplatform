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

import android.graphics.Typeface
import androidx.compose.ui.text.font.TypefaceResult
import androidx.compose.ui.text.font.TypefaceResult.Async
import androidx.compose.ui.text.font.TypefaceResult.Immutable
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

internal class TypefaceResultSubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: TypefaceResult?
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<TypefaceResultSubject?, TypefaceResult?> =
            Factory { failureMetadata, subject -> TypefaceResultSubject(failureMetadata, subject) }
    }

    fun isImmutableTypefaceOf(expectedInstance: Typeface) {
        check("isNotNull())").that(subject).isNotNull()
        check("is TypefaceResult.Immutable").that(subject)
            .isInstanceOf(Immutable::class.java)
        check(".value == $expectedInstance").that(subject?.value)
            .isSameInstanceAs(expectedInstance)
    }

    fun isImmutableTypeface() {
        check("isNotNull())").that(subject).isNotNull()
        check("is TypefaceResult.Immutable").that(subject)
            .isInstanceOf(Immutable::class.java)
    }

    fun currentAsyncTypefaceValue(expectedInstance: Typeface) {
        check("isNotNull())").that(subject).isNotNull()
        check("is TypefaceResult.Async").that(subject)
            .isInstanceOf(Async::class.java)
        check("$subject === $expectedInstance")
            .that(subject?.value)
            .isSameInstanceAs(expectedInstance)
    }

    fun isAsyncTypeface() {
        check("isNotNull())").that(subject).isNotNull()
        check("is TypefaceResult.Async").that(subject)
            .isInstanceOf(Async::class.java)
    }

    override fun actualCustomStringRepresentation(): String = when (subject) {
        null -> "null TypefaceResult"
        is Immutable ->
            "TypefaceResult.Immutable(value=${subject.value})"
        is Async ->
            "TypefaceResult.Immutable(currentState=${subject.current.value})"
    }
}