/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies static browser functions across common, web, and JVM facade emission.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.STRING
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CompanionFunctionEmissionTest {
    @Test
    fun commonCompanionsDeclareCommonFunctions() {
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
        val values = JvmStubValues(mapOf(CSS_CLASS.commonName to CSS_CLASS))
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

private val CSS_MAPPING = CommonPackageMapping(COMMON_CSS_PACKAGE, "Css", "CssDictionaries")

private val CSS_CLASS = CommonClass(
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
    companion = CommonCompanion(
        properties = emptyList(),
        functions = listOf(
            CommonFunction(
                name = "escape",
                parameters = listOf(
                    CommonParameter("ident", STRING, isVararg = false, hasDefault = false),
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
