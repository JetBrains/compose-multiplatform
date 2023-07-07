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
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth.assertAbout

internal fun assertThat(bitmap: Bitmap?): BitmapSubject {
    return assertAbout(BitmapSubject.SUBJECT_FACTORY).that(bitmap)!!
}

internal fun IntegerSubject.isZero() {
    this.isEqualTo(0)
}