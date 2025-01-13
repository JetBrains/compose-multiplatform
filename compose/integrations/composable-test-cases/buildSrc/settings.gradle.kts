dependencyResolutionManagement {
    versionCatalogs {
        val catalog = create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        val kotlinVersion = providers.gradleProperty("kotlin_version").orNull
        if (kotlinVersion != null) {
            catalog.version("kotlin", kotlinVersion)
//            println("buildsrc kotlin version applied: $kotlinVersion")
        }
    }
}