// Verifies deterministic JVM companion constant allocation from the portable model alone.
package prototype.dom.generator

import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class JvmConstantValuesTest {
    @Test
    fun valuesAreSortedAndIndependentOfInputOrder() {
        val constants = listOf(
            constant("ZETA", SHORT),
            constant("ALPHA", SHORT),
            constant("ALPHA", SHORT),
        )
        val forward = JvmConstantValues(constants)
        val reversed = JvmConstantValues(constants.reversed())

        assertEquals("0", forward.render("ALPHA", SHORT))
        assertEquals("1", forward.render("ZETA", SHORT))
        assertEquals(forward.render("ALPHA", SHORT), reversed.render("ALPHA", SHORT))
        assertEquals(forward.render("ZETA", SHORT), reversed.render("ZETA", SHORT))
    }

    @Test
    fun repeatedNamesShareAValueWhileDifferentNamesStayDistinct() {
        val inherited = constant("INHERITED", INT)
        val other = constant("OTHER", INT)
        val once = JvmConstantValues(listOf(inherited, other))
        val repeated = JvmConstantValues(listOf(inherited, other, inherited))

        assertEquals(
            once.render("INHERITED", INT),
            repeated.render("INHERITED", INT),
        )
        assertNotEquals(repeated.render("INHERITED", INT), repeated.render("OTHER", INT))
    }

    @Test
    fun everyNumericTypeHasItsOwnTypeCorrectNamespace() {
        val values = JvmConstantValues(
            listOf(BYTE, SHORT, INT, LONG).map { constant("SAME_NAME", it) },
        )

        assertEquals("0", values.render("SAME_NAME", BYTE))
        assertEquals("0", values.render("SAME_NAME", SHORT))
        assertEquals("0", values.render("SAME_NAME", INT))
        assertEquals("0L", values.render("SAME_NAME", LONG))
    }

    @Test
    fun unsupportedAndExhaustedTypesFailClearly() {
        assertFailsWith<IllegalStateException> {
            JvmConstantValues(listOf(constant("TEXT", STRING)))
        }
        assertFailsWith<IllegalStateException> {
            JvmConstantValues((0..Byte.MAX_VALUE + 1).map { constant("BYTE_$it", BYTE) })
        }
    }

    private fun JvmConstantValues.render(name: String, type: TypeName): String =
        initializer(constant(name, type)).toString()

    private fun constant(name: String, type: TypeName) = PortableConstant(name, type)
}
