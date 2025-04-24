plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
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
    val locales = listOf(
        "", "af", "am", "ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "da", "de",
        "el", "en-rGB", "en-rIN", "es", "es-rUS", "et", "eu", "fa", "fi", "fr", "fr-rCA",
        "gl", "gu", "hi", "hr", "hu", "hy", "in", "is", "it", "iw", "ja", "ka", "kk", "km",
        "kn", "ko", "ky", "lo", "lt", "lv", "mk", "ml", "mn", "mr", "ms", "my", "nb", "ne",
        "nl", "or", "pa", "pl", "pt", "pt-rBR", "pt-rPT", "ro", "ru", "si", "sk", "sl", "sq",
        "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "ur", "uz", "vi", "zh-rCN", "zh-rHK", "zh-rTW"
    )

    doLast {
        val txt = buildString {
            appendLine("<resources>")
            repeat(count) {
                appendLine("    <string name=\"str_${it}\">str_${it}</string>")
            }
            appendLine("</resources>")
        }

        // Generate strings.xml for all locales
        locales.forEach { locale ->
            val dirName = if (locale.isEmpty()) "values" else "values-$locale"
            File(resourcesFolder, "$dirName/strings.xml").apply {
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
