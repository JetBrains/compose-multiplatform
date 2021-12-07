pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

includeBuild("../../templates/desktop-template") {
    name = "subproject"

    dependencySubstitution {
        all {
            val requested = requested as? ModuleComponentSelector ?: return@all
            val group = requested.group
            val module = requested.module
            if (group.startsWith("org.jetbrains.compose")) {
                useTarget(module("$group:$module:1.0.0-alpha2"))
            }
        }
    }
}