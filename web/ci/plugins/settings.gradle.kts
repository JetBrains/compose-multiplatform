pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
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

module(":ci-karmaconf", "karmaconf")
