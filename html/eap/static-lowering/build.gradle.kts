/* Copyright 2026 JetBrains s.r.o. Licensed under the Apache License, Version 2.0. */
plugins {
    kotlin("jvm") version "2.4.20"
    application
}

repositories { mavenCentral() }
kotlin { jvmToolchain(21) }
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.20")
    testImplementation(kotlin("test-junit"))
}
application { mainClass.set("LowerStaticKt") }

// Check rewrites and cases that keep their original source.
tasks.register("checkLowering") {
    dependsOn("installDist", "test")
}
tasks.test { testLogging.showStandardStreams = true }
tasks.named("check") { dependsOn("checkLowering") }
