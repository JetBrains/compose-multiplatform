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

package androidx.compose.runtime.saveable

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AutoSaverTest {

    @Test
    fun simpleSave() {
        val saver = autoSaver<Int>()

        with(saver) {
            assertThat(allowingScope.save(2))
                .isEqualTo(2)
        }
    }
}

val allowingScope = object : SaverScope {
    override fun canBeSaved(value: Any) = true
}

val disallowingScope = object : SaverScope {
    override fun canBeSaved(value: Any) = false
}