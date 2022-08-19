/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.text.empirical

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ParametersKtTest {

    @Test
    fun generate0() {
        assertThat(generateCacheableStringOf(0)).isEqualTo("")
    }

    @Test
    fun generate1() {
        assertThat(generateCacheableStringOf(1)).isEqualTo("a")
    }

    @Test
    fun generate2() {
        assertThat(generateCacheableStringOf(2)).isEqualTo("aa")
    }

    @Test
    fun generate7() {
        assertThat(generateCacheableStringOf(7)).isEqualTo("aaaaaaa")
    }

    @Test
    fun generate8() {
        assertThat(generateCacheableStringOf(8)).isEqualTo("aaaaaaaa")
    }

    @Test
    fun generate9() {
        assertThat(generateCacheableStringOf(9)).isEqualTo("aaaaaaa a")
    }

    @Test
    fun generate10() {
        assertThat(generateCacheableStringOf(10)).isEqualTo("aaaaaaa aa")
    }

    @Test
    fun generate16() {
        assertThat(generateCacheableStringOf(16)).isEqualTo("aaaaaaa aaaaaaaa")
    }
}