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

package androidx.compose.ui.tooling.preview.datasource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoremIpsumTest {
    @Test
    fun testLoremIpsumProvider() {
        assertEquals("", LoremIpsum(0).values.single())
        assertEquals(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer sodales\n" +
                "laoreet",
            LoremIpsum(10).values.single()
        )
        assertEquals(
            2000, LoremIpsum(2000).values.single().split(" ").size
        )
        assertTrue(LoremIpsum().values.single().startsWith("Lorem ipsum dolor sit amet"))
    }
}
