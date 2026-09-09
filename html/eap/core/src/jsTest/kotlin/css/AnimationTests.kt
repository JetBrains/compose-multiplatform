/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.utils.serializeRules
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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

class AnimationsStyleSheetWithCustomPrefix(
    customPrefix: String
) : StyleSheet(customPrefix) {
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
        val generatedRules = AnimationsStyleSheet.serializeRules()


        assertContentEquals(
            listOf(
                "@keyframes AnimationsStyleSheet-bounce { from {  transform: translateX(50%); } to {  transform: translateX(-50%); }}",
                ".AnimationsStyleSheet-animationClass { animation: AnimationsStyleSheet-bounce 2s ease-in alternate;}"
            ),
            generatedRules
        )
    }

    @Test
    fun animationClassInjected() = runTest {
        composition {
            Style(AnimationsStyleSheet)
        }

        val el = nextChild() as HTMLStyleElement
        val cssRules = (el.sheet as? CSSStyleSheet)?.cssRules
        val rules = (0 until (cssRules?.length ?: 0)).map {
            cssRules?.item(it)?.cssText?.replace("\n", "") ?: ""
        }

        // TODO: we need to come up with test that not relying on any kind of formatting
        assertEquals(
            "@keyframes AnimationsStyleSheet-bounce {0% { transform: translateX(50%); }100% { transform: translateX(-50%); }}",
            rules[0].replace("   0%", "0%").replace("  100%", "100%"),
            "Animation keyframes wasn't injected correctly"
        )

        assertEquals(
            ".AnimationsStyleSheet-animationClass { animation: 2s ease-in 0s 1 alternate none running AnimationsStyleSheet-bounce; }".trimIndent(),
            rules[1],
            "Animation class wasn't injected correctly"
        )
    }

    @Test
    fun animationClassInjectedWithCustomPrefix() = runTest {
        val customPrefix = "CustomPrefix-"
        composition {
            Style(AnimationsStyleSheetWithCustomPrefix(customPrefix))
        }

        val el = nextChild() as HTMLStyleElement
        val cssRules = (el.sheet as? CSSStyleSheet)?.cssRules
        val rules = (0 until (cssRules?.length ?: 0)).map {
            cssRules?.item(it)?.cssText?.replace("\n", "") ?: ""
        }

        // TODO: we need to come up with test that not relying on any kind of formatting
        assertEquals(
            "@keyframes ${customPrefix}bounce {0% { transform: translateX(50%); }100% { transform: translateX(-50%); }}",
            rules[0].replace("   0%", "0%").replace("  100%", "100%"),
            "Animation keyframes wasn't injected correctly"
        )

        assertEquals(
            ".${customPrefix}animationClass { animation: 2s ease-in 0s 1 alternate none running ${customPrefix}bounce; }".trimIndent(),
            rules[1],
            "Animation class wasn't injected correctly"
        )
    }
}
