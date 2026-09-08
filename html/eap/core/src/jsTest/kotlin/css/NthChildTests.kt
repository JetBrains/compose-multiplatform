/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.selectors.Nth
import org.jetbrains.compose.web.css.utils.serializeRules
import kotlin.test.Test
import kotlin.test.assertContentEquals

class NthChildTests {
    @Test
    fun nthChildOddTest() {
        val styleSheet = object : StyleSheet(usePrefix = false) {
            val someClass by style {
                color(Color.red)


                nthChild(Nth.Odd) style {
                    color(Color.green)
                }
            }
        }

        assertContentEquals(
            listOf(".someClass { color: red;}", ".someClass :nth-child(odd) { color: green;}"),
            styleSheet.serializeRules()
        )
    }

    @Test
    fun nthChildEvenTest() {
        val styleSheet = object : StyleSheet(usePrefix = false) {
            val someClass by style {
                color(Color.red)


                nthChild(Nth.Even) style {
                    color(Color.green)
                }
            }
        }

        assertContentEquals(
            listOf(".someClass { color: red;}", ".someClass :nth-child(even) { color: green;}"),
            styleSheet.serializeRules()
        )
    }

    @Test
    fun nthChildFunctionalTest() {
        val styleSheet = object : StyleSheet(usePrefix = false) {
            val someClass by style {
                color(Color.red)


                nthChild(Nth.Functional(2, 3)) style {
                    color(Color.green)
                }

                nthChild(Nth.Functional(2)) style {
                    color(Color.green)
                }

                nthChild(Nth.Functional(b = 5)) style {
                    color(Color.green)
                }

            }
        }

        assertContentEquals(
            listOf(
                ".someClass { color: red;}",
                ".someClass :nth-child(2n+3) { color: green;}",
                ".someClass :nth-child(2n) { color: green;}",
                ".someClass :nth-child(5) { color: green;}"
            ),
            styleSheet.serializeRules()
        )
    }
}
