// Verifies static browser functions across common, web, and JVM facade emission.
package prototype.dom.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.STRING
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CompanionFunctionEmissionTest {
    @Test
    fun commonCompanionsDeclarePortableFunctions() {
        val common = commonDeclarationsFile(CSS_MAPPING, listOf(CSS_CLASS), emptyList()).toString()

        assertContains(
            common,
            """
            |public expect abstract class CSS : JsAny {
            |  public companion object {
            |    public fun escape(ident: String): String
            |  }
            |}
            """.trimMargin(),
        )
    }

    @Test
    fun webActualsUseTheBrowserCompanionThroughTheTypealias() {
        val web = browserLeafDeclarationsFile(CSS_MAPPING, listOf(CSS_CLASS), emptyList()).toString()

        assertContains(web, "public actual typealias CSS = CSS")
        assertFalse("fun escape" in web)
    }

    @Test
    fun jvmCompanionsEmitTypeCorrectBodies() {
        val values = JvmStubValues(mapOf(CSS_CLASS.portableName to CSS_CLASS))
        val jvm = jvmDeclarationsFile(
            CSS_MAPPING,
            listOf(CSS_CLASS),
            emptyList(),
            values,
            JvmConstantValues(emptyList()),
        ).toString()

        assertContains(jvm, "public actual fun escape(ident: String): String = \"\"")
    }
}

private val CSS_MAPPING = PortablePackageMapping(PORTABLE_CSS_PACKAGE, "PortableCss", "CssDictionaries")

private val CSS_CLASS = PortableClass(
    browserName = ClassName(DOM_CSS_PACKAGE, "CSS"),
    parentBrowserName = null,
    superinterfaces = emptyList(),
    ancestors = emptyList(),
    shape = ClassShape.ABSTRACT,
    isDictionary = false,
    isJsAny = true,
    properties = emptyList(),
    functions = emptyList(),
    constructors = emptyList(),
    companion = PortableCompanion(
        properties = emptyList(),
        functions = listOf(
            PortableFunction(
                name = "escape",
                parameters = listOf(
                    PortableParameter("ident", STRING, isVararg = false, hasDefault = false),
                ),
                returnType = STRING,
                open = false,
                abstractInBrowser = false,
            ),
        ),
    ),
    factory = null,
    sourceFile = null,
)
