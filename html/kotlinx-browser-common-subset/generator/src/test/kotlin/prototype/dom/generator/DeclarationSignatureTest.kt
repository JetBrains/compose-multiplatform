// Verifies portable declaration signatures.
package prototype.dom.generator

import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.test.Test
import kotlin.test.assertEquals

class DeclarationSignatureTest {
    @Test
    fun numericSequenceDifferencesAreNormalizedByJsDouble() {
        assertEquals(
            "kotlinx.browser.JsArray<kotlinx.browser.JsDouble>",
            PORTABLE_JS_ARRAY.parameterizedBy(PORTABLE_JS_DOUBLE).signature(),
        )
    }

    @Test
    fun aPropertyKeyDoesNotSpellItsMutability() {
        val mutable = property("lineWidth", mutable = true)
        val immutable = property("lineWidth", mutable = false)

        assertEquals("val lineWidth", mutable.key())
        assertEquals(mutable.key(), immutable.key())
    }
}

private fun property(name: String, mutable: Boolean): PortableProperty = PortableProperty(
    name = name,
    type = DOUBLE,
    mutable = mutable,
    open = true,
    abstractInBrowser = true,
)
