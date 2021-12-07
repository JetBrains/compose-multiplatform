package org.jetbrains.compose.gradle

import java.io.File
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma

fun KotlinKarma.standardKarmaConf() {
    println("============================================")
    println("============================================")
    println(File("../../../../test-utils/conf/karma.config.common.d").normalize().absolutePath)
    println(File(".").normalize().absolutePath)
    println(System.getenv("PWD"))
    println("============================================")
    println("============================================")
    //useConfigDirectory("${rootProject.projectDir}/test-utils/conf/karma.config.common.d")
    useChromeHeadless()
    //useFirefox()
}
