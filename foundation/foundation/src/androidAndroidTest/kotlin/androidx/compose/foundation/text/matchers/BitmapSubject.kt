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

package androidx.compose.foundation.text.matchers

import android.graphics.Bitmap
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth extension for Bitmap.
 */
internal class BitmapSubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: Bitmap?
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<BitmapSubject?, Bitmap?> =
            Factory { failureMetadata, subject -> BitmapSubject(failureMetadata, subject) }
    }

    /**
     * Checks the equality of two [Bitmap]s.
     *
     * @param bitmap the [Bitmap] to be matched.
     */
    fun isEqualToBitmap(bitmap: Bitmap) {
        if (subject == bitmap) return
        check("isNotNull()").that(subject).isNotNull()
        check("sameAs()").that(subject!!.sameAs(bitmap)).isTrue()
    }

    /**
     * Checks the inequality of two [Bitmap]s.
     *
     * @param bitmap the [Bitmap] to be matched.
     */
    fun isNotEqualToBitmap(bitmap: Bitmap) {
        if (subject != bitmap) return
        check("sameAs()").that(subject.sameAs(bitmap)).isFalse()
    }

    override fun actualCustomStringRepresentation(): String {
        return if (subject != null) {
            "($subject ${subject.width}x${subject.height} ${subject.config.name})"
        } else {
            super.actualCustomStringRepresentation()
        }
    }
}