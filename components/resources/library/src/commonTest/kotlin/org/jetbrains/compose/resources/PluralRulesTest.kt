/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.plural.*
import org.jetbrains.compose.resources.plural.PluralCategory
import org.jetbrains.compose.resources.plural.PluralRuleList
import kotlin.test.*

/**
 * Tests Unicode CLDR pluralization rules.
 */
class PluralRulesTest {
    /**
     * Tests the actual language pluralization rules with the integer samples given by Unicode.
     */
    @Test
    fun testIntegerSamples() = runTest {
        for ((locale, samplesByCategory) in cldrPluralRuleIntegerSamples) {
            val pluralRuleList = PluralRuleList.getInstance(locale)
            for ((category, samples) in samplesByCategory) {
                for (sample in parsePluralSamples(samples)) {
                    assertEquals(category, pluralRuleList.getCategory(sample))
                }
            }
        }
    }

    @Test
    fun testOrCondition() {
        val pluralRuleList = pluralRuleListOf(
            PluralCategory.ONE to "n = 15 or n = 24"
        )
        repeat(30) {
            if (it == 15 || it == 24) {
                assertEquals(PluralCategory.ONE, pluralRuleList.getCategory(it))
            } else {
                assertEquals(PluralCategory.OTHER, pluralRuleList.getCategory(it))
            }
        }
    }

    @Test
    fun testAndCondition() {
        val pluralRuleList = pluralRuleListOf(
            PluralCategory.ONE to "n = 15 and n = 24"
        )
        repeat(30) {
            assertEquals(PluralCategory.OTHER, pluralRuleList.getCategory(it))
        }
    }

    @Test
    fun testModulus() {
        val pluralRuleList = pluralRuleListOf(
            PluralCategory.ONE to "n % 3 = 2"
        )
        repeat(30) {
            if (it % 3 == 2) {
                assertEquals(PluralCategory.ONE, pluralRuleList.getCategory(it))
            } else {
                assertEquals(PluralCategory.OTHER, pluralRuleList.getCategory(it))
            }
        }
    }

    @Test
    fun testRange() {
        val pluralRuleList = pluralRuleListOf(
            PluralCategory.ONE to "n = 2..3,5,10..24"
        )
        repeat(30) {
            if (it in 2..3 || it == 5 || it in 10..24) {
                assertEquals(PluralCategory.ONE, pluralRuleList.getCategory(it))
            } else {
                assertEquals(PluralCategory.OTHER, pluralRuleList.getCategory(it))
            }
        }
    }

    @Test
    fun testMultipleRules() {
        val pluralRuleList = pluralRuleListOf(
            PluralCategory.ZERO to "n = 0",
            PluralCategory.ONE to "n = 1",
            PluralCategory.TWO to "n = 20",
            PluralCategory.FEW to "n = 300",
            PluralCategory.MANY to "n = 400",
        )
        repeat(500) {
            val expected = when (it) {
                0 -> PluralCategory.ZERO
                1 -> PluralCategory.ONE
                20 -> PluralCategory.TWO
                300 -> PluralCategory.FEW
                400 -> PluralCategory.MANY
                else -> PluralCategory.OTHER
            }
            assertEquals(expected, pluralRuleList.getCategory(it))
        }
    }

    @Test
    fun testOperandValues() {
        pluralRuleListOf(
            PluralCategory.ONE to "n = 1"
        ).run {
            assertEquals(PluralCategory.OTHER, getCategory(-3))
            assertEquals(PluralCategory.OTHER, getCategory(-2))
            assertEquals(PluralCategory.ONE, getCategory(-1))
            assertEquals(PluralCategory.OTHER, getCategory(0))
            assertEquals(PluralCategory.ONE, getCategory(1))
            assertEquals(PluralCategory.OTHER, getCategory(2))
            assertEquals(PluralCategory.OTHER, getCategory(3))
        }

        pluralRuleListOf(
            PluralCategory.ONE to "i = 1"
        ).run {
            assertEquals(PluralCategory.OTHER, getCategory(-3))
            assertEquals(PluralCategory.OTHER, getCategory(-2))
            assertEquals(PluralCategory.ONE, getCategory(-1))
            assertEquals(PluralCategory.OTHER, getCategory(0))
            assertEquals(PluralCategory.ONE, getCategory(1))
            assertEquals(PluralCategory.OTHER, getCategory(2))
            assertEquals(PluralCategory.OTHER, getCategory(3))
        }

        for (condition in arrayOf("v = 0", "w = 0", "f = 0", "t = 0", "e = 0")) {
            pluralRuleListOf(
                PluralCategory.ONE to condition
            ).run {
                for (idx in -100..100) {
                    assertEquals(PluralCategory.ONE, getCategory(idx))
                }
            }
        }
    }
}