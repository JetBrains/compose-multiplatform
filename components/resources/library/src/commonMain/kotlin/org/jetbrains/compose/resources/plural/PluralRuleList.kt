/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.plural

import org.jetbrains.compose.resources.AsyncCache
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier

internal class PluralRuleList(private val rules: Array<PluralRule>) {
    fun getCategory(quantity: Int): PluralCategory {
        return rules.first { rule -> rule.appliesTo(quantity) }.category
    }

    companion object {
        private val cache = AsyncCache<Int, PluralRuleList>()
        private val emptyList = PluralRuleList(emptyArray())

        @OptIn(InternalResourceApi::class)
        suspend fun getInstance(
            languageQualifier: LanguageQualifier,
            regionQualifier: RegionQualifier,
        ): PluralRuleList {
            val cldrLocaleName = buildCldrLocaleName(languageQualifier, regionQualifier) ?: return emptyList
            return getInstance(cldrLocaleName)
        }

        suspend fun getInstance(cldrLocaleName: String): PluralRuleList {
            val listIndex = cldrPluralRuleListIndexByLocale[cldrLocaleName]!!
            return cache.getOrLoad(listIndex) { createInstance(listIndex) }
        }

        @OptIn(InternalResourceApi::class)
        private fun buildCldrLocaleName(
            languageQualifier: LanguageQualifier,
            regionQualifier: RegionQualifier,
        ): String? {
            val localeWithRegion = languageQualifier.language + "_" + regionQualifier.region
            if (cldrPluralRuleListIndexByLocale.containsKey(localeWithRegion)) {
                return localeWithRegion
            }
            if (cldrPluralRuleListIndexByLocale.containsKey(languageQualifier.language)) {
                return languageQualifier.language
            }
            return null
        }

        private fun createInstance(cldrPluralRuleListIndex: Int): PluralRuleList {
            val cldrPluralRuleList = cldrPluralRuleLists[cldrPluralRuleListIndex]
            val pluralRules = cldrPluralRuleList.map { PluralRule(it.first, it.second) }
            return PluralRuleList(pluralRules.toTypedArray())
        }
    }
}