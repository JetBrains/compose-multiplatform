plugins {
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

mavenPublicationConfig {
    displayName = "Compose Preview RPC"
    description = "Compose Preview RPC"
    artifactId = "preview-rpc"
}

dependencies {
    implementation(kotlin("stdlib"))
}

configureAllTests()

val serializeClasspath by tasks.registering(SerializeClasspathTask::class) {
    val runtimeClasspath = configurations.runtimeClasspath
    val jar = tasks.jar
    dependsOn(runtimeClasspath, jar)

    classpathFileCollection.from(jar.flatMap { it.archiveFile })
    classpathFileCollection.from(runtimeClasspath)
    outputFile.set(project.layout.buildDirectory.file("rpc.classpath.txt"))
}

tasks.test.configure {
    dependsOn(serializeClasspath)
    systemProperty(
        "org.jetbrains.compose.tests.rpc.classpath.file",
        serializeClasspath.get().outputFile.get().asFile.absolutePath
    )
}