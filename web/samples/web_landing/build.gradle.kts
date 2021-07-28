plugins {
    kotlin("multiplatform") version "1.5.21"
    id("org.jetbrains.compose") version "0.5.0-build270"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

tasks.register<Sync>("sync") {
    val targetDir = rootProject.projectDir.resolve("examples/${project.projectDir.name}")
    from(project.projectDir)
    into(targetDir)
    doLast {
        println("from ${project.projectDir} => $targetDir")
    }
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(npm("highlight.js", "10.7.2"))
                implementation(compose.web.core)
                implementation(compose.runtime)
            }
        }
    }
}
