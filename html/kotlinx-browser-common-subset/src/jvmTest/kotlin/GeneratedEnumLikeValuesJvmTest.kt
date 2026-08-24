/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies JVM behavior for generated enum-like values.
package kotlinx.browser.dom.enumlike

import kotlinx.browser.JsAny
import kotlinx.browser.dom.AUTO
import kotlinx.browser.dom.ColorSpaceConversion
import kotlinx.browser.dom.DEFAULT
import kotlinx.browser.dom.ImageOrientation
import kotlinx.browser.dom.NONE
import kotlinx.browser.dom.PremultiplyAlpha
import kotlinx.browser.dom.SMOOTH
import kotlinx.browser.dom.ScrollBehavior
import kotlinx.browser.dom.ScrollRestoration
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Verifies the JVM facade emits one private singleton per enum-like value.
class GeneratedEnumLikeValuesJvmTest {
    /** One object per value, whichever way it is reached. */
    @Test
    fun eachValueIsASingleObject() {
        assertSame(ScrollBehavior.AUTO, ScrollBehavior.AUTO)
        assertSame(ScrollBehavior.SMOOTH, ScrollBehavior.SMOOTH)
        assertSame(ImageOrientation.NONE, ImageOrientation.NONE)

        val held: JsAny = ScrollBehavior.AUTO
        assertSame(held, ScrollBehavior.AUTO)
    }

    // Values with the same name remain distinct across enum types.
    @Test
    fun valuesOfDifferentEnumsStayDistinct() {
        assertNotSame<JsAny>(ColorSpaceConversion.NONE, PremultiplyAlpha.NONE)
        assertNotSame<JsAny>(ColorSpaceConversion.NONE, ImageOrientation.NONE)
        assertNotSame<JsAny>(ColorSpaceConversion.DEFAULT, PremultiplyAlpha.DEFAULT)
        // The same name on two enums that also share the *literal* upstream: both are "auto".
        assertNotSame<JsAny>(ScrollBehavior.AUTO, ScrollRestoration.AUTO)
    }

    /** Values do not implement unrelated enum interfaces. */
    @Test
    fun valuesAreNotTypedByOtherEnums() {
        assertTrue(ScrollBehavior.AUTO !is ScrollRestoration)
        assertTrue(ColorSpaceConversion.NONE !is PremultiplyAlpha)
    }

    // Keeps generated singleton implementations out of the public API.
    @Test
    fun theSingletonsAreNotPublic() {
        val singleton = ScrollBehavior.AUTO.javaClass

        assertTrue(singleton.simpleName.startsWith("ScrollBehavior"), "unexpected singleton $singleton")
        assertTrue(
            java.lang.reflect.Modifier.isPublic(singleton.modifiers).not(),
            "$singleton is public, so it leaks out of the facade",
        )
    }
}
