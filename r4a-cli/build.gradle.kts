import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

description = "Kotlin R4A Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.adarshr.test-logger").version("1.2.0")
}

dependencies {
    compileOnly(intellijDep()) { includeJars("extensions", "openapi", "util", "idea", "platform-api", "platform-impl", "android-base-common", rootProject = rootProject) }
    compileOnly(intellijDep()) { includeJars("asm-all") }
    testRuntime(intellijDep())
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))

    compileOnly(project(":compiler:backend-common"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":idea:idea-core"))
    compile(project(":idea:ide-common"))
    compileOnly(project(":idea"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(projectDist(":kotlin-stdlib"))
    testCompile(project(":r4a-runtime"))
    testRuntime(intellijPluginDep("android"))

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))

    testCompile("com.google.android:android:4.1.1.4")
    testCompile("org.robolectric:robolectric:3.8")
    testCompile("org.robolectric:android-all:6.0.0_r1-robolectric-0")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

testsJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName.removePrefix("kotlin-") + ".jar")

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

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

    // The version of maven in the kotlin dependencies is incompatible with robolectric and causes all robolectric tests to fail
    classpath = classpath.filter { !it.name.endsWith("maven-model-3.3.9.jar") }
}
