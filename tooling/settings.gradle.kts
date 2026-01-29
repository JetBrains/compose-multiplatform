pluginManagement {
    repositories {
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        gradlePluginPortal()
    }
}

include(":compose-intellij-platform")
include(":compose-intellij-platform:sample:base")
include(":compose-intellij-platform:sample:plugin-1")
include(":compose-intellij-platform:sample:plugin-2")
include(":compose-intellij-platform:sample:all-plugins")
