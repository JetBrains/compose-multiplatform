package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal object ComposeProperties {
    fun isVerbose(providers: ProviderFactory): Provider<Boolean> = providers
        .gradleProperty("compose.desktop.verbose")
        .orElse("false")
        .map { "true".equals(it, ignoreCase = true) }

    fun preserveWorkingDir(providers: ProviderFactory): Provider<Boolean> = providers
        .gradleProperty("compose.preserve.working.dir")
        .orElse("false")
        .map { "true".equals(it, ignoreCase = true) }
}