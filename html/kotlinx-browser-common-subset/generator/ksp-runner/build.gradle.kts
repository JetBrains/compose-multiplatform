import org.gradle.api.tasks.Sync

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

val kotlinxBrowserVersion = providers.gradleProperty("kotlinx.browser.version").get()
val browserSources by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    browserSources("org.jetbrains.kotlinx:kotlinx-browser:$kotlinxBrowserVersion:sources@jar")
}

val unpackedBrowserSources = layout.buildDirectory.dir(
    "upstream/kotlinx-browser-$kotlinxBrowserVersion/webMain",
)
val unpackKotlinxBrowserSources by tasks.registering(Sync::class) {
    group = "generation"
    description = "Resolves and unpacks the kotlinx-browser $kotlinxBrowserVersion webMain sources."

    from({ zipTree(browserSources.singleFile) }) {
        include("webMain/**")
        eachFile { path = path.removePrefix("webMain/") }
        includeEmptyDirs = false
    }
    into(unpackedBrowserSources)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    js {
        nodejs()
    }

    sourceSets {
        val commonMain by getting
        val webMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir(unpackedBrowserSources)
        }
        val jsMain by getting {
            dependsOn(webMain)
        }
    }
}

dependencies {
    add("kspJs", project(":"))
}

tasks.matching { it.name == "kspKotlinJs" }.configureEach {
    dependsOn(unpackKotlinxBrowserSources)
}

val generatedSubset = layout.buildDirectory.dir("generated/kotlinxBrowserCommonSubset")
val subsetDirectory = rootProject.layout.projectDirectory.dir("..")
val checkedInSources = subsetDirectory.dir("src")
val checkedInManifest = subsetDirectory.file("api/dom-api-manifest.txt")
val generateKotlinxBrowserCommonSubset by tasks.registering(Sync::class) {
    group = "generation"
    description = "Stages generated facade sources from the resolved source artifact."
    dependsOn("kspKotlinJs", unpackKotlinxBrowserSources)

    from(layout.buildDirectory.dir("generated/ksp/js/jsMain/resources")) {
        include("commonDom/**")
        eachFile {
            path = path.removePrefix("commonDom/")
            if (path.endsWith(".kt.txt")) path = path.removeSuffix(".txt")
        }
        includeEmptyDirs = false
    }
    into(generatedSubset)
}

val generatedSourceSets = listOf("commonMain", "jsMain", "wasmJsMain", "jvmMain")
val handwrittenSourcePaths = setOf("kotlinx/browser/Interop.kt")

fun directoryDifferences(
    generated: File,
    checkedIn: File,
    ignoredPaths: Set<String> = emptySet(),
): List<String> {
    fun files(root: File): Map<String, File> = if (!root.isDirectory) {
        emptyMap()
    } else {
        root.walkTopDown()
            .filter(File::isFile)
            .associateBy { it.relativeTo(root).invariantSeparatorsPath }
    }

    val expected = files(generated).filterKeys { it !in ignoredPaths }
    val actual = files(checkedIn).filterKeys { it !in ignoredPaths }
    return buildList {
        (expected.keys - actual.keys).sorted().forEach { add("missing $it") }
        (actual.keys - expected.keys).sorted().forEach { add("unexpected $it") }
        (expected.keys intersect actual.keys).sorted()
            .filterNot { expected.getValue(it).readBytes().contentEquals(actual.getValue(it).readBytes()) }
            .forEach { add("changed $it") }
    }
}

val checkKotlinxBrowserCommonSubset by tasks.registering {
    group = "verification"
    description = "Checks that checked-in sources and the API manifest match fresh generation."
    dependsOn(generateKotlinxBrowserCommonSubset)

    inputs.dir(generatedSubset)
    inputs.dir(checkedInSources)
    inputs.file(checkedInManifest)

    doLast {
        val generated = generatedSubset.get().asFile
        val differences = generatedSourceSets.flatMap { sourceSet ->
            directoryDifferences(
                generated.resolve("$sourceSet/kotlin"),
                checkedInSources.asFile.resolve("$sourceSet/kotlin"),
                handwrittenSourcePaths,
            ).map { "$sourceSet: $it" }
        }.toMutableList()
        if (!generated.resolve("api-manifest.txt").readBytes()
                .contentEquals(checkedInManifest.asFile.readBytes())) {
            differences += "changed api/dom-api-manifest.txt"
        }
        if (differences.isNotEmpty()) {
            val omitted = if (differences.size > 50) {
                "\n... and ${differences.size - 50} more"
            } else {
                ""
            }
            throw GradleException(
                "Checked-in kotlinx-browser common subset is stale:\n" +
                    differences.take(50).joinToString("\n") +
                    omitted +
                    "\nRun updateKotlinxBrowserCommonSubset after reviewing the generated output.",
            )
        }
    }
}

val updateSourceTasks = generatedSourceSets.map { sourceSet ->
    tasks.register<Sync>("update${sourceSet.replaceFirstChar(Char::uppercaseChar)}SubsetSources") {
        group = "generation"
        description = "Updates the checked-in $sourceSet common browser subset sources."
        dependsOn(generateKotlinxBrowserCommonSubset)
        from(generatedSubset.map { it.dir("$sourceSet/kotlin") })
        into(checkedInSources.dir("$sourceSet/kotlin"))
        preserve {
            include("kotlinx/browser/Interop.kt")
        }
    }
}

val updateSubsetManifest by tasks.registering(Sync::class) {
    group = "generation"
    description = "Updates the checked-in common browser subset API manifest."
    dependsOn(generateKotlinxBrowserCommonSubset)
    from(generatedSubset.map { it.file("api-manifest.txt") }) {
        rename { "dom-api-manifest.txt" }
    }
    into(checkedInManifest.asFile.parentFile)
}

tasks.register("updateKotlinxBrowserCommonSubset") {
    group = "generation"
    description = "Explicitly replaces checked-in subset sources and manifest with fresh output."
    dependsOn(updateSourceTasks, updateSubsetManifest)
}
