import org.jetbrains.compose.compose

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    java
    application
}

dependencies {
    implementation(compose.desktop.all)
    implementation(project(":common:utils"))
    implementation(project(":common:database"))
    implementation(project(":common:root"))
    implementation(Deps.ArkIvanov.Decompose.decompose)
    implementation(Deps.ArkIvanov.MVIKotlin.mvikotlin)
    implementation(Deps.ArkIvanov.MVIKotlin.mvikotlinMain)
    implementation(Deps.Badoo.Reaktive.reaktive)
    implementation(Deps.Badoo.Reaktive.coroutinesInterop)
}

application {
    mainClassName = "example.todo.desktop.MainKt"
}
