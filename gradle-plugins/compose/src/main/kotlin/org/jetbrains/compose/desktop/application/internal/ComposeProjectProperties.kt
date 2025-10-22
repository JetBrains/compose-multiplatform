/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.compose.internal.utils.findLocalOrGlobalProperty
import org.jetbrains.compose.internal.utils.toBooleanProvider
import org.jetbrains.compose.internal.utils.valueOrNull
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal object ComposeProperties {
    internal const val VERBOSE = "compose.desktop.verbose"
    internal const val PRESERVE_WD = "compose.preserve.working.dir"
    internal const val MAC_SIGN = "compose.desktop.mac.sign"
    internal const val MAC_SIGN_ID = "compose.desktop.mac.signing.identity"
    internal const val MAC_SIGN_KEYCHAIN = "compose.desktop.mac.signing.keychain"
    internal const val MAC_SIGN_PREFIX = "compose.desktop.mac.signing.prefix"
    internal const val MAC_NOTARIZATION_APPLE_ID = "compose.desktop.mac.notarization.appleID"
    internal const val MAC_NOTARIZATION_PASSWORD = "compose.desktop.mac.notarization.password"
    internal const val MAC_NOTARIZATION_TEAM_ID_PROVIDER = "compose.desktop.mac.notarization.teamID"
    internal const val CHECK_JDK_VENDOR = "compose.desktop.packaging.checkJdkVendor"
    internal const val DISABLE_MULTIMODULE_RESOURCES = "org.jetbrains.compose.resources.multimodule.disable"
    internal const val SYNC_RESOURCES_PROPERTY = "compose.ios.resources.sync"
    internal const val DISABLE_HOT_RELOAD = "org.jetbrains.compose.hot.reload.disable"
    internal const val DISABLE_RESOURCE_CONTENT_HASH_GENERATION = "org.jetbrains.compose.resources.content.hash.generation.disable"

    fun isVerbose(providers: ProviderFactory): Provider<Boolean> =
        providers.valueOrNull(VERBOSE).toBooleanProvider(false)

    fun preserveWorkingDir(providers: ProviderFactory): Provider<Boolean> =
        providers.valueOrNull(PRESERVE_WD).toBooleanProvider(false)

    fun macSign(providers: ProviderFactory): Provider<Boolean> =
        providers.valueOrNull(MAC_SIGN).toBooleanProvider(false)

    fun macSignIdentity(providers: ProviderFactory): Provider<String?> =
        providers.valueOrNull(MAC_SIGN_ID)

    fun macSignKeychain(providers: ProviderFactory): Provider<String?> =
        providers.valueOrNull(MAC_SIGN_KEYCHAIN)

    fun macSignPrefix(providers: ProviderFactory): Provider<String?> =
        providers.valueOrNull(MAC_SIGN_PREFIX)

    fun macNotarizationAppleID(providers: ProviderFactory): Provider<String?> =
        providers.valueOrNull(MAC_NOTARIZATION_APPLE_ID)

    fun macNotarizationPassword(providers: ProviderFactory): Provider<String?> =
        providers.valueOrNull(MAC_NOTARIZATION_PASSWORD)

    fun macNotarizationTeamID(providers: ProviderFactory): Provider<String?> =
        providers.valueOrNull(MAC_NOTARIZATION_TEAM_ID_PROVIDER)

    fun checkJdkVendor(providers: ProviderFactory): Provider<Boolean> =
        providers.valueOrNull(CHECK_JDK_VENDOR).toBooleanProvider(true)

    fun disableMultimoduleResources(providers: ProviderFactory): Provider<Boolean> =
        providers.valueOrNull(DISABLE_MULTIMODULE_RESOURCES).toBooleanProvider(false)

    fun disableHotReload(providers: ProviderFactory): Provider<Boolean> =
        providers.valueOrNull(DISABLE_HOT_RELOAD).toBooleanProvider(false)

    fun disableResourceContentHashGeneration(providers: ProviderFactory): Provider<Boolean> =
        providers.valueOrNull(DISABLE_RESOURCE_CONTENT_HASH_GENERATION).toBooleanProvider(false)

    //providers.valueOrNull works only with root gradle.properties
    fun dontSyncResources(project: Project): Provider<Boolean> =
        project.findLocalOrGlobalProperty(SYNC_RESOURCES_PROPERTY).map { it == "false" }
}