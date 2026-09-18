package org.jetbrains.compose.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma

object kotlinKarmaConfig {
    var rootDir: String? = null
}


fun KotlinKarma.standardConf() {
    if (kotlinKarmaConfig.rootDir == null) {
        throw Exception("kotlinKarmaConfig.rootDir should be set somewhere")
    }
    // The decorated Kotlin reporter is JS-only.
    val configDirectory = when (compilation.platformType) {
        KotlinPlatformType.wasm -> "karma.config.wasm.d"
        else -> "karma.config.common.d"
    }
    useConfigDirectory("${kotlinKarmaConfig.rootDir}/test-utils/conf/$configDirectory")
    useChromeHeadless()
    useFirefoxHeadless()
}
