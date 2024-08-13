/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal.validation

import org.gradle.api.provider.Provider

internal fun validateBundleID(bundleIDProvider: Provider<String?>): String {
    val bundleID = bundleIDProvider.orNull
    check(!bundleID.isNullOrEmpty()) { ERR_BUNDLE_ID_IS_EMPTY }
    check(bundleID.matches("[A-Za-z0-9\\-\\.]+".toRegex())) { ERR_BUNDLE_ID_WRONG_FORMAT }
    return bundleID
}

private const val ERR_PREFIX = "macOS settings error:"
private const val BUNDLE_ID_FORMAT =
    "bundleID may only contain alphanumeric characters (A-Z, a-z, 0-9), hyphen (-) and period (.) characters"
private val ERR_BUNDLE_ID_IS_EMPTY =
    """|$ERR_PREFIX bundleID is empty or null. To specify:
       |  * Use 'nativeDistributions.macOS.bundleID' DSL property;
       |  * $BUNDLE_ID_FORMAT;
       |  * Use reverse DNS notation (e.g. "com.mycompany.myapp");
       |""".trimMargin()
private val ERR_BUNDLE_ID_WRONG_FORMAT =
    "$ERR_PREFIX $BUNDLE_ID_FORMAT"