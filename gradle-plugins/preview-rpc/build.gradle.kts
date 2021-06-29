plugins {
    kotlin("jvm")
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

configureJUnit()

tasks.test.configure {
    configureJavaForComposeTest()

    val runtimeClasspath = configurations.runtimeClasspath
    dependsOn(runtimeClasspath)
    val jar = tasks.jar
    dependsOn(jar)
    doFirst {
        val rpcClasspath = LinkedHashSet<File>()
        rpcClasspath.add(jar.get().archiveFile.get().asFile)
        rpcClasspath.addAll(runtimeClasspath.get().files)
        val classpathString = rpcClasspath.joinToString(File.pathSeparator) { it.absolutePath }
        systemProperty("org.jetbrains.compose.test.rpc.classpath", classpathString)
    }
}