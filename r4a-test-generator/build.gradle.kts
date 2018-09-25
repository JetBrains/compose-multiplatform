
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

dependencies {
    testCompile(projectTests(":idea"))
    compile(projectTests(":generators:test-generator"))
}


projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.r4a.GenerateTestsKt")

testsJar()
