/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose") apply false
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir("../core/src/commonMain/kotlin")
        kotlin.include("org/jetbrains/compose/html/compiler/**")
        kotlin.include("org/jetbrains/compose/web/dom/HtmlSerializationRules.kt")
        kotlin.include("org/jetbrains/compose/web/dom/RawTextTagName.kt")
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.20")
}

tasks.named("check") {
    dependsOn(":sample:test")
}
