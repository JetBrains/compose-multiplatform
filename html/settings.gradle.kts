pluginManagement {
    val COMPOSE_CORE_VERSION = extra["compose.version"] as String
    println("[build] compose core version: $COMPOSE_CORE_VERSION")

    // pluginManagement section won't see outer scope, hence the FQ names
    fun properties(path: String): java.util.Properties? {
        val localPropertiesFile = File(path)
        if (!localPropertiesFile.exists()) {
            return null
        }
        return java.io.FileInputStream(localPropertiesFile).use() { inputStream ->
            val props = java.util.Properties()
            props.load(inputStream)
            props
        }
    }

    val localProperties: java.util.Properties? = properties("local.properties")


    val repos = (localProperties?.getProperty("compose.web.repos"))?.split(File.pathSeparator)

    val buildSrcProperties: java.util.Properties = properties(rootProject.projectDir.resolve("buildSrc/gradle.properties").absolutePath)!!
    val kotlinVersion = buildSrcProperties["kotlin.version"] as String

    repositories {
        gradlePluginPortal()
        mavenCentral()
        repos?.forEach { urlPath ->
            maven {
                url = uri(urlPath)
            }
        }
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ui/dev")
        }
        google()
        mavenLocal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.compose") {
                useModule("org.jetbrains.compose:org.jetbrains.compose.gradle.plugin:$COMPOSE_CORE_VERSION")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.compose") {
                useVersion(kotlinVersion)
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
    }
    versionCatalogs {
        create("libs") {
            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC")
        }
    }
}

fun module(name: String, path: String) {
    include(name)
    val projectDir = rootDir.resolve(path).normalize().absoluteFile
    if (!projectDir.exists()) {
        throw AssertionError("file $projectDir does not exist")
    }
    project(name).projectDir = projectDir
}


module(":html-core", "core")
module(":html-svg", "svg")
module(":html-integration-core", "integration-core")
module(":compose-compiler-integration", "compose-compiler-integration")
module(":compose-compiler-integration-lib", "compose-compiler-integration/lib")
module(":internal-html-core-runtime", "internal-html-core-runtime")
module(":html-test-utils", "test-utils")

if (extra["compose.web.tests.skip.benchmarks"]!!.toString().toBoolean() != true) {
    module(":html-benchmark-core", "benchmark-core")
} else {
    println("skipping benchmarks")
}

if (extra["compose.web.buildSamples"]!!.toString().toBoolean() == true) {
    println("building with examples")
    module(":examples:compose-web-lp", "../examples/web-landing")
    module(":examples:web-compose-bird", "../examples/web-compose-bird")
    module(":examples:web-with-react", "../examples/web-with-react")
}
