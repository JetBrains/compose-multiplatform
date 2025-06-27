includeBuild("..") {
    dependencySubstitution {
        substitute(module("org.jetbrains.compose.internal.build-helpers:publishing"))
            .using(project(":publishing"))
    }
}