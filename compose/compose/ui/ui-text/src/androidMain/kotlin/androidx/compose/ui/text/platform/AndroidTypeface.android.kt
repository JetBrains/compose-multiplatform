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

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import android.graphics.Typeface as NativeTypeface

/**
 * An interface of Android specific Typeface.
 */
internal interface AndroidTypeface : Typeface {
    /**
     * Returns the Android's native Typeface to be able use for given parameters.
     *
     * @param fontWeight A weight to be used for drawing text.
     * @param fontStyle A style to be used for drawing text.
     * @param synthesis An synthesis option for drawing text.
     *
     * @return the Android native Typeface which has closest style to the given parameter.
     */
    fun getNativeTypeface(
        fontWeight: FontWeight,
        fontStyle: FontStyle,
        synthesis: FontSynthesis
    ): NativeTypeface
}
