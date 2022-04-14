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

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.ui.text.font.TypefaceResult
import androidx.compose.ui.geometry.Rect
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth.assertAbout

internal fun assertThat(bitmap: Bitmap?): BitmapSubject {
    return assertAbout(BitmapSubject.SUBJECT_FACTORY).that(bitmap)!!
}

internal fun assertThat(typeface: Typeface?): TypefaceSubject {
    return assertAbout(TypefaceSubject.SUBJECT_FACTORY).that(typeface)!!
}

internal fun assertThat(array: Array<Rect>?): RectArraySubject {
    return assertAbout(RectArraySubject.SUBJECT_FACTORY).that(array)!!
}

internal fun assertThat(rect: Rect?): RectSubject {
    return assertAbout(RectSubject.SUBJECT_FACTORY).that(rect)!!
}

internal fun assertThat(charSequence: CharSequence?): CharSequenceSubject {
    return assertAbout(CharSequenceSubject.SUBJECT_FACTORY).that(charSequence)!!
}

internal fun assertThat(typefaceResult: TypefaceResult?): TypefaceResultSubject {
    return assertAbout(TypefaceResultSubject.SUBJECT_FACTORY).that(typefaceResult)!!
}

internal fun IntegerSubject.isZero() {
    this.isEqualTo(0)
}