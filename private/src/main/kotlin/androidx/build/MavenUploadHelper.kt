/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import androidx.build.Multiplatform.Companion.isMultiplatformEnabled
import com.android.build.gradle.LibraryPlugin
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.stream.JsonWriter
import groovy.util.Node
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.util.StringTokenizer
import org.apache.xerces.jaxp.SAXParserImpl.JAXPSAXParser
import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.DocumentFactory
import org.dom4j.Element
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.xml.sax.InputSource
import org.xml.sax.XMLReader

fun Project.configureMavenArtifactUpload(extension: AndroidXExtension) {
    apply(mapOf("plugin" to "maven-publish"))

    afterEvaluate {
        components.all { component ->
            configureComponent(extension, component)
        }
    }
}

private fun Project.configureComponent(
    extension: AndroidXExtension,
    component: SoftwareComponent
) {
    if (extension.shouldPublish() && component.isAndroidOrJavaReleaseComponent()) {
        val androidxGroup = validateCoordinatesAndGetGroup(extension)
        val projectArchiveDir = File(
            getRepositoryDirectory(),
            "${androidxGroup.group.replace('.', '/')}/$name"
        )
        group = androidxGroup.group

        /*
         * Provides a set of maven coordinates (groupId:artifactId) of artifacts in AndroidX
         * that are Android Libraries.
         */
        val androidLibrariesSetProvider: Provider<Set<String>> = provider {
            val androidxAndroidProjects = mutableSetOf<String>()
            // Check every project is the project map to see if they are an Android Library
            val projectModules = project.getProjectsMap()
            for ((mavenCoordinates, projectPath) in projectModules) {
                project.findProject(projectPath)?.plugins?.hasPlugin(
                    LibraryPlugin::class.java
                )?.let { hasLibraryPlugin ->
                    if (hasLibraryPlugin) {
                        androidxAndroidProjects.add(mavenCoordinates)
                    }
                }
            }
            androidxAndroidProjects
        }

        configure<PublishingExtension> {
            repositories {
                it.maven { repo ->
                    repo.setUrl(getRepositoryDirectory())
                }
            }
            publications {
                if (appliesJavaGradlePluginPlugin()) {
                    // The 'java-gradle-plugin' will also add to the 'pluginMaven' publication
                    it.create<MavenPublication>("pluginMaven")
                    tasks.getByName("publishPluginMavenPublicationToMavenRepository").doFirst {
                        removePreviouslyUploadedArchives(projectArchiveDir)
                    }
                } else {
                    if (!project.isMultiplatformPublicationEnabled()) {
                        it.create<MavenPublication>("maven") {
                            from(component)
                        }
                        tasks.getByName("publishMavenPublicationToMavenRepository").doFirst {
                            removePreviouslyUploadedArchives(projectArchiveDir)
                        }
                    }
                }
            }
            publications.withType(MavenPublication::class.java).all {
                it.pom { pom ->
                    addInformativeMetadata(extension, pom)
                    tweakDependenciesMetadata(androidxGroup, pom, androidLibrariesSetProvider)
                }
            }
        }

        // Register it as part of release so that we create a Zip file for it
        Release.register(this, extension)

        // Workarounds for https://github.com/gradle/gradle/issues/20011
        project.tasks.withType(GenerateModuleMetadata::class.java).configureEach { task ->
            task.doLast {
                val metadataFile = task.outputFile.asFile.get()
                val metadata = metadataFile.readText()
                val sortedMetadata = sortGradleMetadataDependencies(metadata)

                if (metadata != sortedMetadata) {
                    metadataFile.writeText(sortedMetadata)
                }
            }
        }
        project.tasks.withType(GenerateMavenPom::class.java).configureEach { task ->
            task.doLast {
                val pomFile = task.destination
                val pom = pomFile.readText()
                val sortedPom = sortPomDependencies(pom)

                if (pom != sortedPom) {
                    pomFile.writeText(sortedPom)
                }
            }
        }

        // Workaround for https://github.com/gradle/gradle/issues/11717
        project.tasks.withType(GenerateModuleMetadata::class.java).configureEach { task ->
            task.doLast {
                val metadata = task.outputFile.asFile.get()
                val text = metadata.readText()
                metadata.writeText(
                    text.replace(
                        "\"buildId\": .*".toRegex(),
                        "\"buildId:\": \"${getBuildId()}\""
                    )
                )
            }
        }

        if (project.isMultiplatformPublicationEnabled()) {
            configureMultiplatformPublication()
        }
    }
}

/**
 * Looks for a dependencies XML element within [pom] and sorts its contents.
 */
fun sortPomDependencies(pom: String): String {
    // Workaround for using the default namespace in dom4j.
    val namespaceUris = mapOf("ns" to "http://maven.apache.org/POM/4.0.0")
    val docFactory = DocumentFactory()
    docFactory.xPathNamespaceURIs = namespaceUris
    // Ensure that we're consistently using JAXP parser.
    val xmlReader = JAXPSAXParser()
    val document = parseText(docFactory, xmlReader, pom)

    // For each <dependencies> element, sort the contained elements in-place.
    document.rootElement
        .selectNodes("ns:dependencies")
        .filterIsInstance<Element>()
        .forEach { element ->
            val deps = element.elements()
            val sortedDeps = deps.toSortedSet(compareBy { it.stringValue }).toList()

            // Content contains formatting nodes, so to avoid modifying those we replace
            // each element with the sorted element from its respective index. Note this
            // will not move adjacent elements, so any comments would remain in their
            // original order.
            element.content().replaceAll {
                val index = deps.indexOf(it)
                if (index >= 0) {
                    sortedDeps[index]
                } else {
                    it
                }
            }
        }

    // Write to string. Note that this does not preserve the original indent level, but it
    // does preserve line breaks -- not that any of this matters for client XML parsing.
    val stringWriter = StringWriter()
    XMLWriter(stringWriter).apply {
        setIndentLevel(2)
        write(document)
        close()
    }

    return stringWriter.toString()
}

// Coped from org.dom4j.DocumentHelper with modifications to allow SAXReader configuration.
@Throws(DocumentException::class)
fun parseText(
    documentFactory: DocumentFactory,
    xmlReader: XMLReader,
    text: String,
): Document {
    val reader = SAXReader.createDefault()
    reader.documentFactory = documentFactory
    reader.xmlReader = xmlReader
    val encoding = getEncoding(text)
    val source = InputSource(StringReader(text))
    source.encoding = encoding
    val result = reader.read(source)
    if (result.xmlEncoding == null) {
        result.xmlEncoding = encoding
    }
    return result
}

// Coped from org.dom4j.DocumentHelper.
private fun getEncoding(text: String): String? {
    var result: String? = null
    val xml = text.trim { it <= ' ' }
    if (xml.startsWith("<?xml")) {
        val end = xml.indexOf("?>")
        val sub = xml.substring(0, end)
        val tokens = StringTokenizer(sub, " =\"'")
        while (tokens.hasMoreTokens()) {
            val token = tokens.nextToken()
            if ("encoding" == token) {
                if (tokens.hasMoreTokens()) {
                    result = tokens.nextToken()
                }
                break
            }
        }
    }
    return result
}

/**
 * Looks for a dependencies JSON element within [metadata] and sorts its contents.
 */
fun sortGradleMetadataDependencies(metadata: String): String {
    val gson = GsonBuilder().create()
    val jsonObj = gson.fromJson(metadata, JsonObject::class.java)!!
    jsonObj.getAsJsonArray("variants").forEach { entry ->
        (entry as? JsonObject)?.getAsJsonArray("dependencies")?.let { jsonArray ->
            val sortedSet = jsonArray.toSortedSet(compareBy { it.toString() })
            jsonArray.removeAll { true }
            sortedSet.forEach { element -> jsonArray.add(element) }
        }
    }

    val stringWriter = StringWriter()
    val jsonWriter = JsonWriter(stringWriter)
    jsonWriter.setIndent("  ")
    gson.toJson(jsonObj, jsonWriter)
    return stringWriter.toString()
}

private fun Project.isMultiplatformPublicationEnabled(): Boolean {
    if (!project.isMultiplatformEnabled())
        return false
    return extensions.findByType<KotlinMultiplatformExtension>() != null
}

private fun Project.configureMultiplatformPublication() {
    val multiplatformExtension = extensions.findByType<KotlinMultiplatformExtension>()!!

    multiplatformExtension.targets.all { target ->
        if (target is KotlinAndroidTarget) {
            target.publishAllLibraryVariants()
        }
    }
}

private fun SoftwareComponent.isAndroidOrJavaReleaseComponent() =
    name == "release" || name == "java"

private fun Project.validateCoordinatesAndGetGroup(extension: AndroidXExtension): LibraryGroup {
    val mavenGroup = extension.mavenGroup
        ?: throw Exception("You must specify mavenGroup for $name project")
    val strippedGroupId = mavenGroup.group.substringAfterLast(".")
    if (
        !extension.bypassCoordinateValidation &&
        mavenGroup.group.startsWith("androidx") &&
        !name.startsWith(strippedGroupId)
    ) {
        throw Exception("Your artifactId must start with '$strippedGroupId'. (currently is $name)")
    }
    return mavenGroup
}

/**
 * Delete any existing archives, so that developers don't get
 * confused/surprised by the presence of old versions.
 * Additionally, deleting old versions makes it more convenient to iterate
 * over all existing archives without visiting archives having old versions too
 */
private fun removePreviouslyUploadedArchives(projectArchiveDir: File) {
    projectArchiveDir.deleteRecursively()
}

private fun Project.addInformativeMetadata(extension: AndroidXExtension, pom: MavenPom) {
    pom.name.set(extension.name)
    pom.description.set(provider { extension.description })
    pom.url.set(
        provider {
            fun defaultUrl() = "https://developer.android.com/jetpack/androidx/releases/" +
                extension.mavenGroup!!.group.removePrefix("androidx.")
                    .replace(".", "-") +
                "#" + extension.project.version()
            getAlternativeProjectUrl() ?: defaultUrl()
        }
    )
    pom.inceptionYear.set(provider { extension.inceptionYear })
    pom.licenses { licenses ->
        licenses.license { license ->
            license.name.set("The Apache Software License, Version 2.0")
            license.url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            license.distribution.set("repo")
        }
        for (extraLicense in extension.getLicenses()) {
            licenses.license { license ->
                license.name.set(provider { extraLicense.name })
                license.url.set(provider { extraLicense.url })
                license.distribution.set("repo")
            }
        }
    }
    pom.scm { scm ->
        scm.url.set("https://cs.android.com/androidx/platform/frameworks/support")
        scm.connection.set(ANDROID_GIT_URL)
    }
    pom.developers { devs ->
        devs.developer { dev ->
            dev.name.set("The Android Open Source Project")
        }
    }
}

private fun tweakDependenciesMetadata(
    mavenGroup: LibraryGroup,
    pom: MavenPom,
    androidLibrariesSetProvider: Provider<Set<String>>
) {
    pom.withXml { xml ->
        // The following code depends on getProjectsMap which is only available late in
        // configuration at which point Java Library plugin's variants are not allowed to be
        // modified. TODO remove the use of getProjectsMap and move to earlier configuration.
        // For more context see:
        // https://android-review.googlesource.com/c/platform/frameworks/support/+/1144664/8/buildSrc/src/main/kotlin/androidx/build/MavenUploadHelper.kt#177
        assignSingleVersionDependenciesInGroupForPom(xml, mavenGroup)
        assignAarTypes(xml, androidLibrariesSetProvider)
        ensureConsistentJvmSuffix(xml)
    }
}

// TODO(aurimas): remove this when Gradle bug is fixed.
// https://github.com/gradle/gradle/issues/3170
private fun assignAarTypes(
    xml: XmlProvider,
    androidLibrariesSetProvider: Provider<Set<String>>
) {
    val dependencies = xml.asNode().children().find {
        it is Node && it.name().toString().endsWith("dependencies")
    } as Node?

    dependencies?.children()?.forEach { dep ->
        if (dep !is Node) {
            return@forEach
        }
        val groupId = dep.children().first {
            it is Node && it.name().toString().endsWith("groupId")
        } as Node
        val artifactId = dep.children().first {
            it is Node && it.name().toString().endsWith("artifactId")
        } as Node
        if (androidLibrariesSetProvider.get().contains(
                "${groupId.children()[0] as String}:${artifactId.children()[0] as String}"
            )
        ) {
            dep.appendNode("type", "aar")
        }
    }
}

/**
 * Modifies the given .pom to specify that every dependency in <group> refers to a single version
 * and can't be automatically promoted to a new version.
 * This will replace, for example, a version string of "1.0" with a version string of "[1.0]"
 *
 * Note: this is not enforced in Gradle nor in plain Maven (without the Enforcer plugin)
 * (https://github.com/gradle/gradle/issues/8297)
 */
private fun assignSingleVersionDependenciesInGroupForPom(
    xml: XmlProvider,
    mavenGroup: LibraryGroup
) {
    if (!mavenGroup.requireSameVersion) {
        return
    }

    val dependencies = xml.asNode().children().find {
        it is Node && it.name().toString().endsWith("dependencies")
    } as Node?
    dependencies?.children()?.forEach { dep ->
        if (dep !is Node) {
            return@forEach
        }
        val groupId = dep.children().first {
            it is Node && it.name().toString().endsWith("groupId")
        } as Node
        if (groupId.children()[0].toString() == mavenGroup.group) {
            val versionNode = dep.children().first {
                it is Node && it.name().toString().endsWith("version")
            } as Node
            val declaredVersion = versionNode.children()[0].toString()
            if (isVersionRange(declaredVersion)) {
                throw GradleException(
                    "Unsupported version '$declaredVersion': " +
                        "already is a version range"
                )
            }
            val pinnedVersion = "[$declaredVersion]"
            versionNode.setValue(pinnedVersion)
        }
    }
}

private fun isVersionRange(text: String): Boolean {
    return text.contains("[") ||
        text.contains("]") ||
        text.contains("(") ||
        text.contains(")") ||
        text.contains(",")
}

/**
 * Ensures that artifactIds are consistent when using configuration caching.
 * A workaround for https://github.com/gradle/gradle/issues/18369
 */
private fun ensureConsistentJvmSuffix(
    xml: XmlProvider
) {
    val dependencies = xml.asNode().children().find {
        it is Node && it.name().toString().endsWith("dependencies")
    } as Node?
    dependencies?.children()?.forEach { dep ->
        if (dep !is Node) {
            return@forEach
        }
        val artifactIdNode = dep.children().first {
            it is Node && it.name().toString().endsWith("artifactId")
        } as Node
        val artifactId = artifactIdNode.children()[0].toString()
        // kotlinx-coroutines-core is only a .pom and only depends on kotlinx-coroutines-core-jvm,
        // so the two artifacts should be approximately equivalent. However,
        // when loading from configuration cache, Gradle often returns a different resolution.
        // We replace it here to ensure consistency and predictability, and
        // to avoid having to rerun any zip tasks that include it
        if (artifactId == "kotlinx-coroutines-core-jvm") {
            artifactIdNode.setValue("kotlinx-coroutines-core")
        }
    }
}

private fun Project.appliesJavaGradlePluginPlugin() = pluginManager.hasPlugin("java-gradle-plugin")

private const val ANDROID_GIT_URL =
    "scm:git:https://android.googlesource.com/platform/frameworks/support"
