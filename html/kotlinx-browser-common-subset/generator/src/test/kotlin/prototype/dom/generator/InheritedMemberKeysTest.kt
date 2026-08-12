// Verifies that interface contracts are not mistaken for concrete class implementations.
package prototype.dom.generator

import kotlin.test.Test
import kotlin.test.assertEquals

class InheritedMemberKeysTest {
    @Test
    fun aClassOverridesVisibleMembersThatItsParentDoesNotProvide() {
        val inherited = inheritedMemberKeys(
            ClassShape.OPEN,
            parentVisible = setOf("parentMember", "parentContract"),
            parentProvided = setOf("parentMember"),
            interfaceVisible = setOf("directContract"),
        )

        assertEquals(setOf("parentMember"), inherited.deduplication)
        assertEquals(setOf("parentContract", "directContract"), inherited.overrides)
    }

    @Test
    fun anInterfaceInheritsItsParentContractsWithoutRedeclaringThem() {
        val inherited = inheritedMemberKeys(
            ClassShape.INTERFACE,
            parentVisible = emptySet(),
            parentProvided = emptySet(),
            interfaceVisible = setOf("contract"),
        )

        assertEquals(setOf("contract"), inherited.deduplication)
        assertEquals(emptySet(), inherited.overrides)
    }
}
