package prototype.dom.generator

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class SupertypeClosureTest {
    @Test
    fun omittedSupertypeFailsGeneration() {
        val failure = assertFailsWith<IllegalStateException> {
            requireRetainedSupertypes(
                target = "org.example.Target",
                declared = listOf("org.example.Parent", "org.example.OmittedMixin"),
                retained = setOf("org.example.Parent"),
            )
        }

        assertContains(failure.message.orEmpty(), "org.example.Target")
        assertContains(failure.message.orEmpty(), "org.example.OmittedMixin")
        assertContains(failure.message.orEmpty(), "Every direct supertype must be emitted normally")
    }

    @Test
    fun retainedAndInteropSupertypesAreAccepted() {
        requireRetainedSupertypes(
            target = "org.example.Target",
            declared = listOf("org.example.Parent", "kotlin.Any", BROWSER_JS_ANY.canonicalName),
            retained = setOf("org.example.Parent"),
        )
    }
}
