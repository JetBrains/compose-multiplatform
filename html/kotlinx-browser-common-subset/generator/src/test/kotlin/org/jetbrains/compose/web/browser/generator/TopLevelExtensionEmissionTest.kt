/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies that wrapper extensions never expose target-specific browser defaults.
package org.jetbrains.compose.web.browser.generator

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
        val extension = CommonExtensionFunction(
            browserMember = MemberName(DOM_PACKAGE, "get"),
            receiverType = RECEIVER,
            function = CommonFunction(
                name = "get",
                parameters = listOf(
                    CommonParameter(name = "index", type = INT, isVararg = false, hasDefault = true),
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

private val DOM = CommonPackageMapping(COMMON_DOM_PACKAGE, "Dom", "OptionDictionaries")
private val RECEIVER = ClassName(COMMON_DOM_PACKAGE, "DefaultedExtensionReceiver")
