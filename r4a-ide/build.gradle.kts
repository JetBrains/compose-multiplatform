import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

description = "R4A IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.adarshr.test-logger").version("1.2.0")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":r4a-compiler-plugin"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(intellijDep()) { includeJars("openapi", "idea", "util", "extensions", "platform-api", "extensions") }
    //compile(ideaPluginDeps("maven", plugin = "maven"))
    compileOnly(intellijPluginDep("gradle"))

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle")) { isTransitive = false }
    testCompile(project(":idea:idea-maven")) { isTransitive = false }
    testCompile(commonDep("junit:junit"))

    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(projectDist(":kotlin-preloader"))

    testCompile(project(":kotlin-sam-with-receiver-compiler-plugin")) { isTransitive = false }

    testRuntime(project(":plugins:android-extensions-compiler"))
    testRuntime(project(":plugins:android-extensions-ide")) { isTransitive = false }
    testRuntime(project(":allopen-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-allopen-compiler-plugin"))
    testRuntime(project(":noarg-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-noarg-compiler-plugin"))
    testRuntime(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testRuntime(project(":kotlin-scripting-idea")) { isTransitive = false }
    testRuntime(project(":kotlin-scripting-compiler"))
    testRuntime(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    testRuntime(project(":idea:idea-android")) { isTransitive = false }
    testRuntime(project(":plugins:lint")) { isTransitive = false }
    testRuntime(project(":plugins:uast-kotlin"))

    (rootProject.extra["compilerModules"] as Array<String>).forEach {
        testRuntime(project(it))
    }
    
    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }

    testCompile(intellijPluginDep("IntelliLang"))
    testCompile(intellijPluginDep("copyright"))
    testCompile(intellijPluginDep("properties"))
    testCompile(intellijPluginDep("java-i18n"))
    testCompile(intellijPluginDep("stream-debugger"))
    testCompileOnly(intellijDep())
    testCompileOnly(commonDep("com.google.code.findbugs", "jsr305"))
    testCompileOnly(intellijPluginDep("gradle"))
    testCompileOnly(intellijPluginDep("Groovy"))
    testCompileOnly(intellijPluginDep("maven"))

    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("coverage"))
    testRuntime(intellijPluginDep("maven"))
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))
    testRuntime(intellijPluginDep("testng"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

ideaPlugin()

testsJar {}

projectTest {
    workingDir = rootDir
    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
    }

    useAndroidSdk()
    useAndroidJar()
}


tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
        events = setOf(TestLogEvent.FAILED,
                       TestLogEvent.PASSED,
                       TestLogEvent.SKIPPED,
                       TestLogEvent.STANDARD_ERROR,
                       TestLogEvent.STANDARD_OUT)
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
}

configureInstrumentation()