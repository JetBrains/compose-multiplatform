/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.stringPresentation
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertContains

object AnimationsStyleSheet : StyleSheet() {
    val bounce by keyframes {
        from {
            property("transform", "translateX(50%)")
        }

        to {
            property("transform", "translateX(-50%)")
        }
    }

    val animationClass by style {
        animation(bounce) {
            duration(2.s)
            timingFunction(AnimationTimingFunction.EaseIn)
            direction(AnimationDirection.Alternate)
        }
    }
}

@ExperimentalComposeWebApi
class AnimationTests {
    @Test
    fun animationClassGenerated() = runTest {
        val generatedRules = AnimationsStyleSheet.cssRules.map { it.stringPresentation() }

        assertContains(
            generatedRules,
            """
                @keyframes AnimationsStyleSheet-bounce {
                    from {
                        transform: translateX(50%);
                    }
                    to {
                        transform: translateX(-50%);
                    }
                }
            """.trimIndent(),
            "Animation keyframes wasn't generated correctly"
        )
        assertContains(
            generatedRules,
            """
                .AnimationsStyleSheet-animationClass {
                    animation: AnimationsStyleSheet-bounce 2s ease-in alternate;
                }
            """.trimIndent(),
            "Animation class wasn't generated correctly"
        )
    }

    @Test
    fun animationClassInjected() = runTest {
        composition {
            Style(AnimationsStyleSheet)
        }

        val el = root.children[0] as HTMLStyleElement
        val cssRules = (el.sheet as? CSSStyleSheet)?.cssRules
        val rules = (0 until (cssRules?.length ?: 0)).map {
            cssRules?.item(it)?.cssText ?: ""
        }

        assertContains(
            rules,
            """
                @keyframes AnimationsStyleSheet-bounce { 
                  0% { transform: translateX(50%); }
                  100% { transform: translateX(-50%); }
                }
            """.trimIndent(),
            "Animation keyframes wasn't injected correctly"
        )
        assertContains(
            rules,
            """
                .AnimationsStyleSheet-animationClass { animation: 2s ease-in 0s 1 alternate none running AnimationsStyleSheet-bounce; }
            """.trimIndent(),
            "Animation class wasn't injected correctly"
        )
    }
}
