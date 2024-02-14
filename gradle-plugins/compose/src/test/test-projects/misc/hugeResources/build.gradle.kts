plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "app.group"

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.material)
                implementation(compose.components.resources)
            }
        }
    }
}

val generateResourceFiles = tasks.register("generateResourceFiles") {
    val resourcesFolder = project.file("src/commonMain/composeResources")
    val count = 25_000
    doLast {
        val txt = buildString {
            appendLine("<resources>")
            repeat(count) {
                appendLine("    <string name=\"str_${it}\">str_${it}</string>")
            }
            appendLine("</resources>")
        }
        File(resourcesFolder, "values/strings.xml").apply {
            parentFile.mkdirs()
            writeText(txt)
        }
    }
    doLast {
        repeat(count) {
            File(resourcesFolder, "drawable/icon_$it.xml").apply {
                parentFile.mkdirs()
                createNewFile() //empty file
            }
        }
    }
}

tasks.named("generateComposeResClass") {
    dependsOn(generateResourceFiles)
}
