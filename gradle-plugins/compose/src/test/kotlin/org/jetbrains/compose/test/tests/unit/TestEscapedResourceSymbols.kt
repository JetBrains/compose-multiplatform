package org.jetbrains.compose.test.tests.unit

import org.jetbrains.compose.resources.handleSpecialCharacters
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEscapedResourceSymbols {

    @Test
    fun testEscapedSymbols() {
        assertEquals(
            "abc \n \\n \t \\t \u1234 \ua45f \\u1234 \\ \\u355g",
            handleSpecialCharacters("""abc \n \\n \t \\t \u1234 \ua45f \\u1234 \\ \u355g""")
        )
    }
}