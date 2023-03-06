import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.project
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

val Project.isInIdea: Boolean
    get() {
        return System.getProperty("idea.active")?.toBoolean() == true
    }

val Project.isFailingJsCase: Boolean
    get() = this.name.contains("-failingJs-")

@OptIn(ExternalVariantApi::class)
fun KotlinMultiplatformExtension.configureTargets() {
    val addMinGw = project.isInIdea

    jvm("desktop")
    configureJsTargets()
    ios()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosX64()
    macosArm64()
    // We use linux agents on CI. So it doesn't run the tests, but it builds the klib anyway which is time consuming.
    if (addMinGw) mingwX64()
    linuxX64()

    val commonMain = sourceSets.getByName("commonMain")
    val nativeMain = sourceSets.create("nativeMain")
    nativeMain.dependsOn(commonMain)

    val iosMain = sourceSets.getByName("iosMain")
    iosMain.dependsOn(nativeMain)
    sourceSets.getByName("iosSimulatorArm64Main").dependsOn(iosMain)
    sourceSets.getByName("iosArm64Main").dependsOn(iosMain)
    sourceSets.getByName("iosX64Main").dependsOn(iosMain)

    val macosMain = sourceSets.create("macosMain")
    macosMain.dependsOn(nativeMain)
    sourceSets.getByName("macosX64Main").dependsOn(macosMain)
    sourceSets.getByName("macosArm64Main").dependsOn(macosMain)

    val linuxMain = sourceSets.create("linuxMain")
    linuxMain.dependsOn(nativeMain)
    sourceSets.getByName("linuxX64Main").dependsOn(linuxMain)

    if (addMinGw) {
        sourceSets.getByName("mingwX64Main").dependsOn(nativeMain)
    }

}

fun KotlinMultiplatformExtension.configureJsTargets() {
    js(IR) {
        browser()
        // nodejs() // Commented to save a bit of CI time. Testing in a browser should be enough.
    }
}

@OptIn(ExternalVariantApi::class)
fun KotlinDependencyHandler.getLibDependencyForMain(): ProjectDependency {
    if (!project.name.endsWith("-main")) error("Unexpected main module name: ${project.name}")
    return project(":" + project.name.replace("-main", "-lib"))
}

@OptIn(ExternalVariantApi::class)
fun KotlinDependencyHandler.getCommonLib(): ProjectDependency {
    return project(":common")
}

fun KotlinSourceSet.configureCommonTestDependencies() {
    dependencies {
        implementation(kotlin("test"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    }
}
