import java.util.Locale

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
    val count = 1500
    val numberOfLanguages = 20
    val langs = Locale.getAvailableLocales()
        .map { it.language }
        .filter { it.count() == 2 }
        .sorted()
        .distinct()
        .take(numberOfLanguages)
        .toList()
    doLast {
        val txt = buildString {
            appendLine("<resources>")
            repeat(count) {
                appendLine("    <string name=\"str_${it}\">str_${it}</string>")
            }
            appendLine("</resources>")
        }
        langs.forEachIndexed { langIndex, lang ->
            val stringsFileName = if (langIndex == 0) "values/strings.xml" else "values-$lang/strings.xml"
            File(resourcesFolder, stringsFileName).apply {
                parentFile.mkdirs()
                writeText(txt)
            }
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
