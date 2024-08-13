pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
        mavenLocal()
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

enum class CasesToRun(val value: String) {
    Default("default"),
    FailingJs("failingJs"),
    ;

    fun isDefault() = this == Default
}

val casesToRun = CasesToRun.values().firstOrNull {
    it.value == extra["tests.casesToRun"]
} ?: CasesToRun.Default


val listOfFailingJsCases = (extra.properties.getOrDefault("tests.failing.kjs", "") as String).split(",")
val failingJsSuffix = "failingJs"

fun getFailingSuffix(testCaseName: String): String? {
    if (casesToRun == CasesToRun.FailingJs &&
        listOfFailingJsCases.contains(testCaseName)
    ) {
        return failingJsSuffix
    }
    return null
}

fun addRememberAnonymousObjTestCase(testFailingJs: Boolean = false) {
    val libName = ":testcase-rememberAnonymousObj-lib".let {
        if (testFailingJs) {
            it.replace("-lib", "-$failingJsSuffix-lib")
        } else {
            it
        }
    }
    val mainName = ":testcase-rememberAnonymousObj-main".let {
        if (testFailingJs) {
            it.replace("-main", "-$failingJsSuffix-main")
        } else {
            it
        }
    }
    module(libName, "testcases/rememberAnonymousObj/lib")
    module(mainName, "testcases/rememberAnonymousObj/main")
}

/**
 * @param name - the name of a test case
 * @param failingTestCaseNameSuffix - the suffix to add for a failing test case.
 * Each platform should have its own unique suffix.
 */
fun addATestCase(name: String, failingTestCaseNameSuffix: String? = null) {
    val libName = ":testcase-$name-lib".let {
        if (failingTestCaseNameSuffix != null) {
            it.replace("-lib", "-$failingTestCaseNameSuffix-lib")
        } else {
            it
        }
    }
    val mainName = ":testcase-$name-main".let {
        if (failingTestCaseNameSuffix != null) {
            it.replace("-main", "-$failingTestCaseNameSuffix-main")
        } else {
            it
        }
    }

    println("adding $libName, $mainName")
    module(libName, "testcases/$name/lib")
    module(mainName, "testcases/$name/main")
}

include(":common")

if (casesToRun.isDefault()) {
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
}

/**
 * Below we add modules for cases which are known to be failing at least on 1 platform.
 * These cases should be fixed!
 */

"rememberAnonymousObj".also {
    addATestCase(name = it, failingTestCaseNameSuffix = getFailingSuffix(it))
}
