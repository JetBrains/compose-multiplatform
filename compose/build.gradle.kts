import org.gradle.api.*
import org.jetbrains.compose.internal.publishing.*


plugins {
    signing
}

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

OELPublishingPrototype(project)

//if (project.experimentalOELPublication() && (project.oelAndroidxVersion() == null)) {
//    error("androidx version should be specified for OEL publications")
//}
//if (project.experimentalOELPublication())
//    OELPublishingPrototype(project)
//}

val mainComponents =
    listOf(
        ComposeComponent(":compose:animation:animation"),
        ComposeComponent(":compose:animation:animation-core"),
        ComposeComponent(":compose:animation:animation-graphics", supportedPlatforms = ComposePlatforms.JVM_BASED),
        ComposeComponent(":compose:foundation:foundation"),
        ComposeComponent(":compose:foundation:foundation-layout"),
        ComposeComponent(":compose:material:material"),
        ComposeComponent(":compose:material3:material3", supportedPlatforms = ComposePlatforms.JVM_BASED),
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
    publish(
        ":compose:ui:ui-tooling-data",
        onlyWithPlatforms = setOf(ComposePlatforms.AndroidRelease, ComposePlatforms.AndroidDebug),
        publications = listOf("Maven")
    )

    publish(
        ":compose:desktop:desktop",
        onlyWithPlatforms = setOf(ComposePlatforms.Desktop),
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

val mavenCentral = MavenCentralProperties(project)
if (mavenCentral.signArtifacts) {
    signing.useInMemoryPgpKeys(
        mavenCentral.signArtifactsKey.get(),
        mavenCentral.signArtifactsPassword.get()
    )
}

val publishingDir = project.layout.buildDirectory.dir("publishing")
val originalArtifactsRoot = publishingDir.map { it.dir("original") }
val preparedArtifactsRoot = publishingDir.map { it.dir("prepared") }
val modulesFile = publishingDir.map { it.file("modules.txt") }

val findComposeModules by tasks.registering(FindModulesInSpaceTask::class) {
    requestedGroupId.set("org.jetbrains.compose")
    requestedVersion.set(mavenCentral.version)
    spaceInstanceUrl.set("https://public.jetbrains.space")
    spaceClientId.set(System.getenv("COMPOSE_REPO_USERNAME") ?: "")
    spaceClientSecret.set(System.getenv("COMPOSE_REPO_KEY") ?: "")
    spaceProjectId.set(System.getenv("COMPOSE_DEV_REPO_PROJECT_ID") ?: "")
    spaceRepoId.set(System.getenv("COMPOSE_DEV_REPO_REPO_ID") ?: "")
    modulesTxtFile.set(modulesFile)
}

val downloadArtifactsFromComposeDev by tasks.registering(DownloadFromSpaceMavenRepoTask::class) {
    dependsOn(findComposeModules)
    modulesToDownload.set(project.provider { readComposeModules(modulesFile, originalArtifactsRoot) })
    spaceRepoUrl.set("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val fixModulesBeforePublishing by tasks.registering(FixModulesBeforePublishingTask::class) {
    dependsOn(downloadArtifactsFromComposeDev)
    inputRepoDir.set(originalArtifactsRoot)
    outputRepoDir.set(preparedArtifactsRoot)
}

val reuploadArtifactsToMavenCentral by tasks.registering(UploadToSonatypeTask::class) {
    dependsOn(fixModulesBeforePublishing)

    version.set(mavenCentral.version)
    modulesToUpload.set(project.provider { readComposeModules(modulesFile, preparedArtifactsRoot) })

    sonatypeServer.set("https://oss.sonatype.org")
    user.set(mavenCentral.user)
    password.set(mavenCentral.password)
    autoCommitOnSuccess.set(mavenCentral.autoCommitOnSuccess)
    stagingProfileName.set("org.jetbrains.compose")
}

fun readComposeModules(
    modulesFile: Provider<out FileSystemLocation>,
    repoRoot: Provider<out FileSystemLocation>
): List<ModuleToUpload> =
    modulesFile.get().asFile.readLines()
        .filter { it.isNotBlank() }
        .map { line ->
            val (group, artifact, version) = line.split(":")
            ModuleToUpload(
                groupId = group,
                artifactId = artifact,
                version = version,
                localDir = repoRoot.get().asFile.resolve("$group/$artifact/$version")
            )
        }

fun Project.experimentalOELPublication() : Boolean = findProperty("oel.publication") == "true"
fun Project.oelAndroidxVersion() : String? = findProperty("oel.androidx.version") as String?

// FIXME: reflection access! Some API in Kotlin is needed
@Suppress("unchecked_cast")
private val KotlinTarget.kotlinComponents: Iterable<KotlinTargetComponent>
    get() = javaClass.kotlin.memberProperties
        .single { it.name == "kotlinComponents" }
            .get(this) as Iterable<KotlinTargetComponent>


fun OELPublishingPrototype(project: Project) {
    val ext = project.multiplatformExtension ?: error("expected a multiplatform project")

    ext.targets.all { target ->
        if (target is KotlinAndroidTarget) {
            project.publishAndroidxReference(target)
        }
    }
}

@Suppress("unchecked_cast")
private fun Project.publishAndroidxReference(target: KotlinTarget) {
    afterEvaluate {
        target.kotlinComponents.forEach { component ->
            val componentName = component.name

            val multiplatformExtension =
                extensions.findByType(KotlinMultiplatformExtension::class.java)
                    ?: error("Expected a multiplatform project")

            if (component is KotlinVariant)
                component.publishable = false

            val usages = when (component) {
                is KotlinVariant -> component.usages
                is JointAndroidKotlinTargetComponent -> component.usages
                else -> emptyList()
            }

            extensions.getByType(PublishingExtension::class.java)
                .publications.withType(DefaultMavenPublication::class.java)
                // isAlias is needed for Gradle to ignore the fact that there's a
                // publication that is not referenced as an available-at variant of the root module
                // and has the Maven coordinates that are different from those of the root module
                // FIXME: internal Gradle API! We would rather not create the publications,
                //        but some API for that is needed in the Kotlin Gradle plugin
                .all { publication ->
                    if (publication.name == componentName) {
                        publication.isAlias = true
                    }
                }

            usages.forEach {    usage ->
                val configurationName = usage.name + "-published"

                configurations.matching{it.name == configurationName}.all() { conf ->
                    conf.artifacts.clear()
                    conf.dependencies.clear()
                    conf.setExtendsFrom(emptyList())
                    var version = if (target.project.group.toString().contains("org.jetbrains.compose.material3")) "1.0.0-alpha01" else target.project.oelAndroidxVersion()!!
                    val newDependency = target.project.group.toString().replace("org.jetbrains.compose", "androidx.compose") + ":" + name + ":" + version
                    conf.dependencies.add(target.project.dependencies.create(newDependency))
                }

                val rootComponent : KotlinSoftwareComponent = target.project.components.withType(KotlinSoftwareComponent::class.java)
                    .getByName("kotlin")

                (rootComponent.usages as MutableSet).add(
                    DefaultKotlinUsageContext(
                        multiplatformExtension.metadata().compilations.getByName("main"),
                        objects.named(Usage::class.java, "kotlin-api"),
                        configurationName
                    )
                )

            }
        }
    }
}
