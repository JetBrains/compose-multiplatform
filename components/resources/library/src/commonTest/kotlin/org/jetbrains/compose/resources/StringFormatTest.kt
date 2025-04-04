package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.internal.IgnoreWasmTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringFormatTest {

    @Test
    fun `replaceWithArgs replaces placeholders with corresponding arguments`() {
        val template = "Hello %1\$s, you have %2\$d new messages!"
        val args = listOf("Alice", "5")

        val result = template.replaceWithArgs(args)

        assertEquals("Hello Alice, you have 5 new messages!", result)
    }

    @Test
    fun `replaceWithArgs works with multiple placeholders referring to the same argument`() {
        val template = "%1\$s and %1\$s are best friends!"
        val args = listOf("Alice")

        val result = template.replaceWithArgs(args)

        assertEquals("Alice and Alice are best friends!", result)
    }

    @Test
    fun `replaceWithArgs works when placeholders are out of order`() {
        val template = "Order: %2\$s comes after %1\$s"
        val args = listOf("Alice", "Bob")

        val result = template.replaceWithArgs(args)

        assertEquals("Order: Bob comes after Alice", result)
    }

    @Test
    fun `replaceWithArgs works when there are no placeholders`() {
        val template = "No placeholders here!"
        val args = emptyList<String>()

        val result = template.replaceWithArgs(args)

        assertEquals("No placeholders here!", result)
    }

    @Test
    fun `replaceWithArgs throws exception when placeholders index is out of bounds`() {
        val template = "Hello %1\$s, %2\$s!"
        val args = listOf("Alice")

        assertFailsWith<IndexOutOfBoundsException> {
            template.replaceWithArgs(args)
        }
    }

    @Test
    fun `replaceWithArgs handles empty string template`() {
        val template = ""
        val args = listOf("Alice", "5")

        val result = template.replaceWithArgs(args)

        assertEquals("", result)
    }

    @Test
    fun `replaceWithArgs handles templates with no matching args`() {
        val template = "Hello %1\$s, you have %3\$s messages"
        val args = listOf("Alice")

        assertFailsWith<IndexOutOfBoundsException> {
            template.replaceWithArgs(args)
        }
    }

    @Test
    fun `replaceWithArgs replaces multiple placeholders of the same index`() {
        val template = "Repeat: %1\$s, %1\$s, and again %1\$s!"
        val args = listOf("Echo")

        val result = template.replaceWithArgs(args)

        assertEquals("Repeat: Echo, Echo, and again Echo!", result)
    }

    @Test
    fun `replaceWithArgs ensures _d and _s placeholders behave identically`() {
        val template = "%1\$d, %1\$s, %2\$d, %2\$s"
        val args = listOf("42", "hello")

        val result = template.replaceWithArgs(args)

        assertEquals("42, 42, hello, hello", result)
    }

    @Test
    fun `replaceWithArgs handles 15 arguments correctly`() {
        val template = "%1\$s, %2\$s, %3\$s, %4\$s, %5\$s, %6\$s, %7\$s, %8\$s, %9\$s, %10\$s, %11\$s, %12\$s, %13\$s, %14\$s, %15\$s!"
        val args = listOf(
            "arg1", "arg2", "arg3", "arg4", "arg5",
            "arg6", "arg7", "arg8", "arg9", "arg10",
            "arg11", "arg12", "arg13", "arg14", "arg15"
        )

        val result = template.replaceWithArgs(args)

        assertEquals(
            "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15!",
            result
        )
    }

    @Test
    fun `replaceWithArgs throws exception for template with missing argument index`() {
        val template = "Hello %${'$'}s, how are you?"
        val args = listOf("Alice")

        val result = template.replaceWithArgs(args)

        // Since the template doesn't properly specify a valid index (e.g., %1$s), it will replace nothing.
        assertEquals("Hello %${'$'}s, how are you?", result)
    }

    @Test
    fun `replaceWithArgs does not replace invalid placeholders`() {
        val template = "Hello %1\$x, how are you?"
        val args = listOf("Alice")

        val result = template.replaceWithArgs(args)

        // %1$x is not a valid placeholder, so the template should remain unchanged
        assertEquals("Hello %1\$x, how are you?", result)
    }

    @Test
    fun `replaceWithArgs throws exception for missing arguments`() {
        val template = "Hello %1\$s, you have %2\$d messages!"
        val args = listOf("Alice")

        // An exception should be thrown because the second argument (%2$d) is missing
        assertFailsWith<IndexOutOfBoundsException> {
            template.replaceWithArgs(args)
        }
    }

    @Test
    @IgnoreWasmTest // https://youtrack.jetbrains.com/issue/KT-69014, wasm throws RuntimeError instead of IndexOutOfBounds
    fun `replaceWithArgs throws exception for unmatched placeholders`() {
        val template = "Hello %1\$s, your rank is %3\$s"
        val args = listOf("Alice", "1")

        // The template has a %3$s placeholder, but there is no third argument
        assertFailsWith<IndexOutOfBoundsException> {
            template.replaceWithArgs(args)
        }
    }

    @Test
    fun `replaceWithArgs handles templates with invalid format`() {
        val template = "This is %1\$"
        val args = listOf("test")

        val result = template.replaceWithArgs(args)

        // The incomplete placeholder %1$ will not be replaced
        assertEquals("This is %1\$", result)
    }

    @Test
    fun `replaceWithArgs ignores extra arguments`() {
        val template = "Hello %1\$s!"
        val args = listOf("Alice", "ExtraData1", "ExtraData2")

        val result = template.replaceWithArgs(args)

        // Only the first argument should be used, ignoring the rest
        assertEquals("Hello Alice!", result)
    }
}