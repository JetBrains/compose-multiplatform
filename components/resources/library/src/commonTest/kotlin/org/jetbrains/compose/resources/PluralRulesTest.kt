/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.intl.PluralRuleList
import kotlin.test.*

class PluralRulesTest {
    /**
     * Test pluralization for integer quantities using the samples in Unicode CLDR.
     */
    @Test
    fun testIntegerSamples() {
        for ((locale, samplesByCategory) in cldrPluralRuleIntegerSamples) {
            val pluralRuleList = PluralRuleList.createInstance(locale)
            for ((category, samples) in samplesByCategory) {
                for (sample in parsePluralSamples(samples)) {
                    assertEquals(category, pluralRuleList.getCategory(sample))
                }
            }
        }
    }
}