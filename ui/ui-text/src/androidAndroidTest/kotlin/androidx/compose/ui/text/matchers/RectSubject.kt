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

import androidx.compose.ui.geometry.Rect
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

internal class RectSubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: Rect?
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<RectSubject?, Rect?> =
            Factory { failureMetadata, subject -> RectSubject(failureMetadata, subject) }
    }

    fun isEqualToWithTolerance(expected: Rect) {
        if (subject == null) failWithoutActual(simpleFact("is null"))
        check("instanceOf()").that(subject).isInstanceOf(Rect::class.java)
        val tolerance = 0.01f
        assertThat(subject!!.left).isWithin(tolerance).of(expected.left)
        assertThat(subject.top).isWithin(tolerance).of(expected.top)
        assertThat(subject.right).isWithin(tolerance).of(expected.right)
        assertThat(subject.bottom).isWithin(tolerance).of(expected.bottom)
    }
}

internal class RectArraySubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: Array<Rect>?
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<RectArraySubject?, Array<Rect>?> =
            Factory { failureMetadata, subject -> RectArraySubject(failureMetadata, subject) }
    }

    fun isEqualToWithTolerance(expected: Array<Rect>) {
        if (subject == null) failWithoutActual(simpleFact("is null"))
        check("instanceOf()").that(subject).isInstanceOf(Array<Rect>::class.java)
        check("size").that(subject).hasLength(expected.size)
        for (index in subject!!.indices) {
            androidx.compose.ui.text.matchers.assertThat(subject[index])
                .isEqualToWithTolerance(expected[index])
        }
    }
}
