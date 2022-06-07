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

import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@SmallTest
@Suppress("DEPRECATION")
class AndroidTypefaceCacheTest {

    val context = InstrumentationRegistry.getInstrumentation().targetContext!!

    @Test
    fun cached_instance_for_the_same_input() {
        assertThat(
            AndroidTypefaceCache.getOrCreate(context, FontTestData.FONT_100_REGULAR)
        ).isSameInstanceAs(
            AndroidTypefaceCache.getOrCreate(context, FontTestData.FONT_100_REGULAR)
        )
    }

    @Test
    fun not_cached_instance_if_different_input() {
        assertThat(
            AndroidTypefaceCache.getOrCreate(context, FontTestData.FONT_100_REGULAR)
        ).isNotSameInstanceAs(
            AndroidTypefaceCache.getOrCreate(context, FontTestData.FONT_200_REGULAR)
        )
    }
}