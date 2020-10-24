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
package androidx.compose.ui.text.android.style

import android.text.TextPaint
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class ShadowSpanTest {
    @Test
    fun updateDrawStateTest() {
        val color = -0xff0100
        val offsetX = 1f
        val offsetY = 2f
        val radius = 3f
        val shadowSpan = ShadowSpan(color, offsetX, offsetY, radius)
        val textPaint = Mockito.mock(TextPaint::class.java)
        shadowSpan.updateDrawState(textPaint)
        Mockito.verify(textPaint, Mockito.times(1))
            .setShadowLayer(radius, offsetX, offsetY, color)
    }
}