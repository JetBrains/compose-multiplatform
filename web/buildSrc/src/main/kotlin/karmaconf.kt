package org.jetbrains.compose.gradle

import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma

object kotlinKarmaConfig {
    var rootDir: String? = null
}


fun KotlinKarma.standardConf() {
    if (kotlinKarmaConfig.rootDir == null) {
        throw Exception("kotlinKarmaConfig.rootDir should be set somewhere")
    }
    useConfigDirectory("${kotlinKarmaConfig.rootDir}/test-utils/conf/karma.config.common.d")
    useChromeHeadless()
    useFirefoxHeadless()
}
