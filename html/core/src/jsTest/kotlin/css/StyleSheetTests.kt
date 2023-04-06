/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.utils.serializeRules
import kotlin.test.Test
import kotlin.test.assertContentEquals


class StyleSheetTests {

    @Test
    fun extendExistingStyleSheet() {
        val styleSheet = object : StyleSheet(usePrefix = false) {
            val someClassName by style {
                color(Color.red)
            }
        }

        val childStyleSheet = object : StyleSheet(styleSheet, usePrefix = false) {
            val someClassName by style {
                color(Color.green)
            }
        }

        assertContentEquals(
            listOf(".someClassName { color: red;}", ".someClassName { color: green;}"),
            styleSheet.serializeRules(),
            "styleSheet rules"
        )

        assertContentEquals(
            listOf(".someClassName { color: red;}", ".someClassName { color: green;}"),
            childStyleSheet.serializeRules(),
            "childStyleSheet rules"
        )
    }

}