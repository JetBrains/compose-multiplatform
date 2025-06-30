pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://packages.jetbrains.team/maven/p/kt/dev")
        maven("https://redirector.kotlinlang.org/maven/dev")
        maven {
            url = uri("${rootDir}/build/maven-project")
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        register("libs").configure {
            val kotlinVersion = providers.gradleProperty("kotlin.version").orNull
            if (kotlinVersion != null) {
                version("kotlin", kotlinVersion)
//                println("kotlin version applied: $kotlinVersion")
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

module(":testcase-valueClass-lib", "testcases/valueClass/lib")
module(":testcase-valueClass-main", "testcases/valueClass/main")

module(":testcase-lambdas-lib", "testcases/lambdas/lib")
module(":testcase-lambdas-main", "testcases/lambdas/main")

module(":testcase-expectActual-lib", "testcases/expectActual/lib")
module(":testcase-expectActual-main", "testcases/expectActual/main")

module(":testcase-stability-lib", "testcases/stability/lib")
module(":testcase-stability-main", "testcases/stability/main")
