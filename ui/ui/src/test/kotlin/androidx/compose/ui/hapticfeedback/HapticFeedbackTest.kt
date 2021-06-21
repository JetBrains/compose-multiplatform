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

package androidx.compose.ui.hapticfeedback

import android.view.HapticFeedbackConstants
import android.view.View
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HapticFeedbackTest {
    @Test
    fun HapticFeedBack_TextHandleMove_Constant() {
        val view = spy(View(mock()))
        val hapticFeedBack = PlatformHapticFeedback(view)

        hapticFeedBack.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        verify(
            view,
            times(1)
        ).performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
    }

    @Test
    fun HapticFeedBack_LongPress_constant() {
        val view = spy(View(mock()))
        val hapticFeedBack = PlatformHapticFeedback(view)

        hapticFeedBack.performHapticFeedback(HapticFeedbackType.LongPress)

        verify(
            view,
            times(1)
        ).performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}