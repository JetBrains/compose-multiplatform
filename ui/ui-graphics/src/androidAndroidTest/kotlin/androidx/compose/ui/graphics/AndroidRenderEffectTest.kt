/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidRenderEffectTest {

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testRenderEffectSupported() {
        assertTrue(BlurEffect(5f, 10f).isSupported())
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.R)
    fun testRenderEffectNotSupported() {
        assertFalse(BlurEffect(1f, 2f).isSupported())
    }
}