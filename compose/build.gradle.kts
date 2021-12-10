import org.gradle.api.*

fun Task.dependsOnComposeTask(name: String) {
   dependsOn(project.composeBuild?.task(name) ?: return)
}

open class ComposePublishingTask : AbstractComposePublishingTask() {
    override fun dependsOnComposeTask(task: String) {
        dependsOn(project.composeBuild?.task(task) ?: return)
    }
}

val composeProperties = ComposeProperties(project)
val isWebExist =
    project.composeBuild?.run { projectDir.resolve(".jbWebExistsMarker").exists() } ?: false

val mainComponents =
    listOf(
        ComposeComponent(":compose:animation:animation"),
        ComposeComponent(":compose:animation:animation-core"),
        ComposeComponent(":compose:foundation:foundation"),
        ComposeComponent(":compose:foundation:foundation-layout"),
        ComposeComponent(":compose:material:material"),
        ComposeComponent(":compose:material:material-icons-core"),
        ComposeComponent(":compose:material:material-ripple"),
        ComposeComponent(":compose:runtime:runtime"),
        ComposeComponent(":compose:runtime:runtime-saveable"),
        ComposeComponent(":compose:ui:ui"),
        ComposeComponent(":compose:ui:ui-geometry"),
        ComposeComponent(":compose:ui:ui-graphics"),
        ComposeComponent(":compose:ui:ui-test", supportedPlatforms = ComposePlatforms.JVM_BASED),
        ComposeComponent(":compose:ui:ui-test-junit4", supportedPlatforms = ComposePlatforms.JVM_BASED),
        ComposeComponent(":compose:ui:ui-text"),
        ComposeComponent(":compose:ui:ui-tooling", supportedPlatforms = ComposePlatforms.JVM_BASED),
        ComposeComponent(":compose:ui:ui-tooling-preview", supportedPlatforms = ComposePlatforms.JVM_BASED),
        ComposeComponent(":compose:ui:ui-unit"),
        ComposeComponent(":compose:ui:ui-util", supportedPlatforms = ComposePlatforms.ALL),
    )

val iconsComponents =
    listOf(
        ComposeComponent(":compose:material:material-icons-extended", supportedPlatforms = ComposePlatforms.JVM_BASED),
    )

fun ComposePublishingTask.mainPublications() {
    publish(":compose:compiler:compiler", publications = listOf("Maven"))
    publish(":compose:compiler:compiler-hosted", publications = listOf("Maven"))
    publish(":compose:ui:ui-tooling-data", publications = listOf("Maven"))

    publish(
        ":compose:desktop:desktop",
        publications = listOf(
            "KotlinMultiplatform",
            "Jvm",
            "Jvmlinux-x64",
            "Jvmlinux-arm64",
            "Jvmmacos-x64",
            "Jvmmacos-arm64",
            "Jvmwindows-x64"
        )
    )

    mainComponents.forEach { publishMultiplatform(it) }
}

fun ComposePublishingTask.iconsPublications() {
    iconsComponents.forEach { publishMultiplatform(it) }
}

// To show all projects which use `xxx` task, run:
// ./gradlew -p frameworks/support help --task xxx
tasks.register("publishComposeJb", ComposePublishingTask::class) {
    repository = "MavenRepository"
    mainPublications()
}

tasks.register("publishComposeJbToMavenLocal", ComposePublishingTask::class) {
    repository = "MavenLocal"
    mainPublications()
}

// separate task that cannot be built in parallel (because it requires too much RAM).
// should be run with "--max-workers=1"
tasks.register("publishComposeJbExtendedIcons", ComposePublishingTask::class) {
    repository = "MavenRepository"
    iconsPublications()
}

tasks.register("publishComposeJbExtendedIconsToMavenLocal", ComposePublishingTask::class) {
    repository = "MavenLocal"
    iconsPublications()
}

tasks.register("testComposeJbDesktop") {
    dependsOnComposeTask(":compose:desktop:desktop:jvmTest")
    dependsOnComposeTask(":compose:animation:animation:desktopTest")
    dependsOnComposeTask(":compose:animation:animation-core:desktopTest")
    dependsOnComposeTask(":compose:ui:ui:desktopTest")
    dependsOnComposeTask(":compose:ui:ui-graphics:desktopTest")
    dependsOnComposeTask(":compose:ui:ui-text:desktopTest")
    dependsOnComposeTask(":compose:foundation:foundation:desktopTest")
    dependsOnComposeTask(":compose:foundation:foundation-layout:desktopTest")
    dependsOnComposeTask(":compose:material:material:desktopTest")
    dependsOnComposeTask(":compose:material:material-ripple:desktopTest")
    dependsOnComposeTask(":compose:runtime:runtime:desktopTest")
    dependsOnComposeTask(":compose:runtime:runtime-saveable:desktopTest")
}

if (isWebExist) {
    tasks.register("testComposeJbWeb") {
        dependsOnComposeTask(":compose:runtime:runtime:jsTest")
        dependsOnComposeTask(":compose:runtime:runtime:test")
    }
}

tasks.register("buildNativeDemo") {
    dependsOnComposeTask(":compose:native:demo:assemble")
}

tasks.register("testRuntimeNative") {
    dependsOnComposeTask(":compose:runtime:runtime:macosX64Test")
}

tasks.register("testComposeModules") { // used in https://github.com/JetBrains/androidx/tree/jb-main/.github/workflows
    // TODO: download robolectrict to run ui:ui:test
    // dependsOnComposeTask(":compose:ui:ui:test")

    dependsOnComposeTask(":compose:ui:ui-graphics:test")
    dependsOnComposeTask(":compose:ui:ui-geometry:test")
    dependsOnComposeTask(":compose:ui:ui-unit:test")
    dependsOnComposeTask(":compose:ui:ui-util:test")
    dependsOnComposeTask(":compose:runtime:runtime:test")
    dependsOnComposeTask(":compose:runtime:runtime-saveable:test")
    dependsOnComposeTask(":compose:material:material:test")
    dependsOnComposeTask(":compose:material:material-ripple:test")
    dependsOnComposeTask(":compose:foundation:foundation:test")
    dependsOnComposeTask(":compose:animation:animation:test")
    dependsOnComposeTask(":compose:animation:animation-core:test")
    dependsOnComposeTask(":compose:animation:animation-core:test")

    // TODO: enable ui:ui-text:test
    // dependsOnComposeTask(":compose:ui:ui-text:test")
    // compose/out/androidx/compose/ui/ui-text/build/intermediates/tmp/manifest/test/debug/tempFile1ProcessTestManifest10207049054096217572.xml Error:
    // android:exported needs to be explicitly specified for <activity>. Apps targeting Android 12 and higher are required to specify an explicit value for `android:exported` when the corresponding component has an intent filter defined.
}

tasks.register("run") {
    dependsOnComposeTask(":compose:desktop:desktop:desktop-samples:run")
}
