import TaskUtils.useAndroidJar
import TaskUtils.useAndroidSdk
import javax.xml.ws.Endpoint.publish
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

description = "R4A Runtime"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.adarshr.test-logger").version("1.2.0")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly("com.google.android:android:4.1.1.4")
    testCompile("com.google.android:android:4.1.1.4")
    testCompile("org.robolectric:robolectric:3.8")
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()

dist(targetName = "r4a-runtime.jar")

publish()

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