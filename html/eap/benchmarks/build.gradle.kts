plugins {
    application
}

val composeVersion = providers.gradleProperty("compose.version").get()
val browserVersion = providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()
val jmhVersion = "1.37"

dependencies {
    implementation(project(":html-core-eap"))
    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
    implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:$browserVersion")
    implementation("org.openjdk.jmh:jmh-core:$jmhVersion")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:$jmhVersion")
}

// Match the library's Java compatibility.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

application {
    mainClass.set("org.openjdk.jmh.Main")
}

tasks.named<JavaExec>("run") {
    args("-prof", "gc")
}
