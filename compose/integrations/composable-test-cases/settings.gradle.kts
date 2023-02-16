pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenLocal()
    }

    plugins {
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

rootProject.name = "composable-test-cases"

fun module(name: String, path: String) {
    include(name)
    val projectDir = rootDir.resolve(path).normalize().absoluteFile
    if (!projectDir.exists()) {
        throw AssertionError("file $projectDir does not exist")
    }
    project(name).projectDir = projectDir
}

gradle.startParameter.setContinueOnFailure(true)

include(":common")

module(":testcase-template-lib", "testcases/template/lib")
module(":testcase-template-main", "testcases/template/main")

module(":testcase-inheritance-composableInterface-lib", "testcases/inheritance/composableInterface/lib")
module(":testcase-inheritance-composableInterface-main", "testcases/inheritance/composableInterface/main")

module(":testcase-inheritance-funInterface-lib", "testcases/inheritance/funInterface/lib")
module(":testcase-inheritance-funInterface-main", "testcases/inheritance/funInterface/main")

module(":testcase-constructors-lib", "testcases/constructors/lib")
module(":testcase-constructors-main", "testcases/constructors/main")

module(":testcase-anonymousObjects-lib", "testcases/anonymousObjects/lib")
module(":testcase-anonymousObjects-main", "testcases/anonymousObjects/main")
