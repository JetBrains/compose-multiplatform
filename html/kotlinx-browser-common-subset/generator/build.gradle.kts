plugins {
    kotlin("jvm")
}

group = "org.jetbrains.compose.html.build"
version = "1.0"

dependencies {
    implementation(
        "com.google.devtools.ksp:symbol-processing-api:${providers.gradleProperty("ksp.version").get()}",
    )
    implementation("com.squareup:kotlinpoet-jvm:2.2.0")
    testImplementation(kotlin("test"))
}

val generatedCommonSource = project(":ksp-runner").layout.buildDirectory.file(
    "generated/kotlinxBrowserCommonSubset/commonMain/kotlin/kotlinx/browser/dom/PortableDom.kt",
)
val generatedCommonMetadata = project(":verification").layout.buildDirectory.file(
    "classes/kotlin/metadata/commonMain/default/manifest",
)

tasks.test {
    dependsOn(":verification:compileCommonMainKotlinMetadata")
    inputs.file(generatedCommonSource).withPropertyName("generatedPortableDomCommonSource")
    inputs.file(generatedCommonMetadata).withPropertyName("generatedPortableDomCommonMetadata")
    systemProperty("portableDomCommonSource", generatedCommonSource.get().asFile.absolutePath)
    systemProperty("portableDomCommonMetadata", generatedCommonMetadata.get().asFile.absolutePath)
}
