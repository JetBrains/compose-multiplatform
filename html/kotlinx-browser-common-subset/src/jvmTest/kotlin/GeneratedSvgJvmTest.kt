/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies the inert JVM SVG hierarchy used for common generic bounds.
import java.lang.reflect.Modifier
import kotlinx.browser.css.masking.SVGClipPathElement
import kotlinx.browser.css.masking.SVGMaskElement
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.svg.SVGCircleElement
import kotlinx.browser.dom.svg.SVGElement
import kotlinx.browser.dom.svg.SVGGeometryElement
import kotlinx.browser.dom.svg.SVGGraphicsElement
import kotlin.test.Test
import kotlin.test.assertTrue

class GeneratedSvgJvmTest {
    @Test
    fun svgTypesPreserveTheirAbstractElementHierarchy() {
        assertTrue(Element::class.java.isAssignableFrom(SVGElement::class.java))
        assertTrue(SVGElement::class.java.isAssignableFrom(SVGGraphicsElement::class.java))
        assertTrue(SVGGraphicsElement::class.java.isAssignableFrom(SVGGeometryElement::class.java))
        assertTrue(SVGGeometryElement::class.java.isAssignableFrom(SVGCircleElement::class.java))
        assertTrue(SVGElement::class.java.isAssignableFrom(SVGClipPathElement::class.java))
        assertTrue(SVGElement::class.java.isAssignableFrom(SVGMaskElement::class.java))

        listOf(
            SVGElement::class,
            SVGGraphicsElement::class,
            SVGGeometryElement::class,
            SVGCircleElement::class,
            SVGClipPathElement::class,
            SVGMaskElement::class,
        ).forEach { type ->
            assertTrue(Modifier.isAbstract(type.java.modifiers), "${type.simpleName} must remain inert")
        }
    }
}
