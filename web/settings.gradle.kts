pluginManagement {
    val COMPOSE_CORE_VERSION: String by settings
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

    repositories {
        gradlePluginPortal()
        mavenCentral()
        repos?.forEach { urlPath ->
            maven {
                url = uri(urlPath)
            }
        }
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ui/dev")
        }

        google()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.compose") {
                useModule("org.jetbrains.compose:org.jetbrains.compose.gradle.plugin:$COMPOSE_CORE_VERSION")
            } else if (requested.id.id == "org.jetbrains.kotlin.multiplatform") {
                useModule("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.6.21")
            }
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


module(":web-core", "core")
module(":web-svg", "svg")
module(":web-widgets", "widgets")
module(":web-integration-core", "integration-core")
module(":web-integration-widgets", "integration-widgets")
module(":compose-compiler-integration", "compose-compiler-integration")
module(":internal-web-core-runtime", "internal-web-core-runtime")
module(":test-utils", "test-utils")

if (extra["compose.web.tests.skip.benchmarks"]!!.toString().toBoolean() != true) {
    module(":web-benchmark-core", "benchmark-core")
} else {
    println("skipping benchmarks")
}

if (extra["compose.web.buildSamples"]!!.toString().toBoolean() == true) {
    println("building with examples")
    module(":examples:compose-web-lp", "../examples/web-landing")
    module(":examples:web-compose-bird", "../examples/web-compose-bird")
    module(":examples:web-with-react", "../examples/web-with-react")
}
