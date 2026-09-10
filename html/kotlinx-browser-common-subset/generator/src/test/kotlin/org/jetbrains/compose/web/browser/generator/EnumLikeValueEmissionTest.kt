/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies target-specific emission of browser string-enum values.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EnumLikeValueEmissionTest {
    /** Declares common values on the facade companion used by callers. */
    @Test
    fun commonDeclarationsExtendTheFacadeCompanion() {
        val common = commonValuesFile(DOM, listOf(AUTO, SMOOTH, EMPTY)).toString()

        assertContains(common, "public expect val ScrollBehavior.Companion.AUTO: ScrollBehavior")
        assertContains(common, "public expect val ScrollBehavior.Companion.SMOOTH: ScrollBehavior")
        assertContains(common, "public expect val CanPlayTypeResult.Companion.EMPTY: CanPlayTypeResult")
        // Upstream declares these `inline`; the facade has nothing to inline and does not repeat it.
        assertFalse("inline" in common)
    }

    /** Forwards through the browser companion so the facade receiver remains resolvable. */
    @Test
    fun webActualsForwardThroughTheBrowserCompanion() {
        val web = targetValuesFile(DOM, listOf(AUTO, SMOOTH)).toString()

        assertContains(web, "import org.w3c.dom.ScrollBehavior as BrowserScrollBehavior")
        assertContains(web, "import org.w3c.dom.AUTO as browserAUTO")
        assertContains(
            web,
            """
            |public actual val BrowserScrollBehavior.Companion.AUTO: ScrollBehavior
            |  get() = BrowserScrollBehavior.browserAUTO
            """.trimMargin(),
        )
        // One aliased import per classifier.
        assertEquals(1, Regex("as BrowserScrollBehavior").findAll(web).count())
    }

    /** Reuses one JVM singleton per value to preserve referential identity. */
    @Test
    fun jvmActualsReturnOneSingletonPerValue() {
        val jvm = jvmValuesFile(DOM, listOf(AUTO, SMOOTH, EMPTY)).toString()

        assertContains(jvm, "private object ScrollBehaviorAuto : ScrollBehavior")
        assertContains(jvm, "private object ScrollBehaviorSmooth : ScrollBehavior")
        assertContains(jvm, "private object CanPlayTypeResultEmpty : CanPlayTypeResult")
        assertContains(
            jvm,
            """
            |public actual val ScrollBehavior.Companion.AUTO: ScrollBehavior
            |  get() = ScrollBehaviorAuto
            """.trimMargin(),
        )
        // Distinct objects, so two values of the same enum never compare equal.
        assertEquals(3, Regex("private object").findAll(jvm).count())
    }

    /** Values of different enums keep distinct singleton names even where the value name is shared. */
    @Test
    fun singletonNamesAreQualifiedByTheirEnum() {
        val none = enumValue("ImageOrientation", "NONE")
        val otherNone = enumValue("PremultiplyAlpha", "NONE")

        assertEquals("kotlinx.browser.dom.ImageOrientationNone", none.jvmSingleton.canonicalName)
        assertEquals("kotlinx.browser.dom.PremultiplyAlphaNone", otherNone.jvmSingleton.canonicalName)
    }

    /** Uses a source-independent null default for nullable dictionary values. */
    @Test
    fun aNullableDictionaryValueDefaultsToNull() {
        val common = commonDictionariesFile(DOM, listOf(SCROLL_TO_OPTIONS)).toString()

        assertContains(
            common,
            "public expect fun ScrollToOptions(behavior: ScrollBehavior? = null): ScrollToOptions",
        )
    }

    /** A null default needs the parameter type but not a cross-package enum-value import. */
    @Test
    fun aNullDefaultFromAnotherFacadePackageImportsOnlyTheType() {
        val common = commonDictionariesFile(EVENTS, listOf(SCROLL_TO_OPTIONS)).toString()

        assertContains(common, "import kotlinx.browser.dom.ScrollBehavior")
        assertFalse("import kotlinx.browser.dom.AUTO" in common)
        assertContains(common, "behavior: ScrollBehavior? = null")
    }
}

private val DOM = CommonPackageMapping(COMMON_DOM_PACKAGE, "Dom", "OptionDictionaries")
private val EVENTS = CommonPackageMapping(COMMON_EVENTS_PACKAGE, "Events", "EventDictionaries")

private fun enumValue(owner: String, name: String): CommonExtensionValue = CommonExtensionValue(
    browserMember = MemberName(DOM_PACKAGE, name),
    browserOwner = ClassName(DOM_PACKAGE, owner),
    name = name,
    sourceFile = null,
)

private val AUTO = enumValue("ScrollBehavior", "AUTO")
private val SMOOTH = enumValue("ScrollBehavior", "SMOOTH")
private val EMPTY = enumValue("CanPlayTypeResult", "EMPTY")

/** `ScrollToOptions`, reduced to one nullable dictionary parameter. */
private val SCROLL_TO_OPTIONS = CommonClass(
    browserName = ClassName(DOM_PACKAGE, "ScrollToOptions"),
    parentBrowserName = null,
    superinterfaces = emptyList(),
    ancestors = emptyList(),
    shape = ClassShape.INTERFACE,
    isDictionary = true,
    isJsAny = true,
    properties = emptyList(),
    functions = emptyList(),
    constructors = emptyList(),
    companion = null,
    factory = CommonFactory(
        parameters = listOf(
            CommonParameter(
                name = "behavior",
                type = ClassName(COMMON_DOM_PACKAGE, "ScrollBehavior").copy(nullable = true),
                isVararg = false,
                hasDefault = true,
            ),
        ),
    ),
    sourceFile = null,
)
