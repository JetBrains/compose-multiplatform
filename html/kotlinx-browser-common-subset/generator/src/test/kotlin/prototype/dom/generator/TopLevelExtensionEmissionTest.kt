// Verifies that wrapper extensions never expose target-specific browser defaults.
package prototype.dom.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.STRING
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class TopLevelExtensionEmissionTest {
    @Test
    fun defaultedBrowserParameterBecomesExplicitOnEveryFacadeTarget() {
        val extension = PortableExtensionFunction(
            browserMember = MemberName(DOM_PACKAGE, "get"),
            receiverType = RECEIVER,
            function = PortableFunction(
                name = "get",
                parameters = listOf(
                    PortableParameter(name = "index", type = INT, isVararg = false, hasDefault = true),
                ),
                returnType = STRING,
                open = false,
                abstractInBrowser = false,
            ),
            sourceFile = null,
        )
        val common = commonDeclarationsFile(DOM, emptyList(), listOf(extension)).toString()
        val web = browserLeafDeclarationsFile(DOM, emptyList(), listOf(extension)).toString()
        val jvm = jvmDeclarationsFile(
            DOM,
            emptyList(),
            listOf(extension),
            JvmStubValues(emptyMap()),
            JvmConstantValues(emptyList()),
        ).toString()

        listOf(common, web, jvm).forEach { output ->
            assertContains(output, "index: Int")
            assertFalse("index: Int =" in output, output)
            assertFalse("definedExternally" in output, output)
        }
        assertFalse("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS" in jvm, jvm)
    }
}

private val DOM = PortablePackageMapping(PORTABLE_DOM_PACKAGE, "PortableDom", "OptionDictionaries")
private val RECEIVER = ClassName(PORTABLE_DOM_PACKAGE, "DefaultedExtensionReceiver")
