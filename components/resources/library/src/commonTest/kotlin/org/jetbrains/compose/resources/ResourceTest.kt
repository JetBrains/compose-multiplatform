/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

@OptIn(ExperimentalResourceApi::class)
class ResourceTest {
    @Test
    fun testResourceEquals() = runBlockingTest {
        assertEquals(getPathById("a"), getPathById("a"))
    }

    @Test
    fun testResourceNotEquals() = runBlockingTest {
        assertNotEquals(getPathById("a"), getPathById("b"))
    }

    @Test
    fun testMissingResource() = runBlockingTest {
        assertFailsWith<MissingResourceException> {
            readResourceBytes("missing.png")
        }
        val error = assertFailsWith<IllegalStateException> {
            loadString("unknown_id")
        }
        assertEquals("String ID=`unknown_id` is not found!", error.message)
    }

    @Test
    fun testReadFileResource() = runBlockingTest {
        val bytes = readResourceBytes("strings.xml")
        assertEquals(
            """
                <resources>
                    <string name="app_name">Compose Resources App</string>
                    <string name="hello">😊 Hello world!</string>
                    <string name="str_template">Hello, %1${'$'}s! You have %2${'$'}d new messages.</string>
                    <string-array name="str_arr">
                        <item>item 1</item>
                        <item>item 2</item>
                        <item>item 3</item>
                    </string-array>
                </resources>
                
            """.trimIndent(),
            bytes.decodeToString()
        )
    }

    @Test
    fun testLoadStringResource() = runBlockingTest {
        assertEquals("Compose Resources App", loadString("app_name"))
        assertEquals(
            "Hello, test-name! You have 42 new messages.",
            loadString("str_template", "test-name", 42)
        )
        assertEquals(listOf("item 1", "item 2", "item 3"), loadStringArray("str_arr"))
    }
}
