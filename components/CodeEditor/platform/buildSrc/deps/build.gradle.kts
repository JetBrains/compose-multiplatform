/*
 * Copyright 2017-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Modified by Alex Hosh (n34to0@gmail.com) 2021.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import java.io.Closeable
import java.io.OutputStreamWriter
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Arrays
import java.util.Date
import javax.xml.stream.XMLOutputFactory
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.extension

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.ow2.asm:asm-commons:9.2")
        classpath("org.ow2.asm:asm-util:9.2")
    }
}

plugins {
    base
}

val intellijVersion = rootProject.extra["intellijSdk.version"] as String
val intellijPlatform = rootProject.extra["intellijSdk.platform"] as String
val customDepsFolder = rootProject.extra["deps.rootFolder"] as String
val customDepsOrg = rootProject.extra["deps.organization"] as String

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com")
}

val intellij: Configuration by configurations.creating
val sources: Configuration by configurations.creating

val localDependenciesDir = rootProject.gradle.gradleUserHomeDir.resolve(customDepsFolder)
val localRepoDir = localDependenciesDir.resolve("repo")
val repoDir = File(localRepoDir, customDepsOrg)

dependencies {
    intellij("com.jetbrains.intellij.idea:$intellijPlatform:$intellijVersion")
    sources("com.jetbrains.intellij.idea:$intellijPlatform:$intellijVersion:sources@jar")
}

fun prepareDeps(
    intellij: Configuration,
    sources: Configuration,
    intellijVersion: String
) {
    val mergeSources = tasks.create("mergeSources${intellij.name.capitalize()}", Jar::class.java) {
        dependsOn(sources)
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        isZip64 = true
        from(provider { sources.map(::zipTree) })
        destinationDirectory.set(File(repoDir, sources.name))
        archiveBaseName.set("intellij")
        archiveClassifier.set("sources")
        archiveVersion.set(intellijVersion)
    }

    val sourcesFile = mergeSources.outputs.files.singleFile

    val makeIde = run {
        val task = buildIvyRepositoryTask(intellij, customDepsOrg, localRepoDir, sourcesFile)

        task.configure {
            dependsOn(mergeSources)
        }

        task
    }

    tasks.named("build") {
        dependsOn(makeIde)
    }
}

prepareDeps(intellij, sources, intellijVersion)

tasks.named<Delete>("clean") {
    delete(localDependenciesDir)
}

fun buildIvyRepositoryTask(
    configuration: Configuration,
    organization: String,
    repoDirectory: File,
    sources: File? = null
): TaskProvider<Task> {
    fun ResolvedArtifact.storeDirectory(): CleanableStore =
        CleanableStore[repoDirectory.resolve("$organization/${moduleVersion.id.name}").absolutePath]

    fun ResolvedArtifact.moduleDirectory(): File =
        storeDirectory()[moduleVersion.id.version].use()

    return tasks.register("buildIvyRepositoryFor${configuration.name.capitalize()}") {
        dependsOn(configuration)
        inputs.files(configuration)

        outputs.upToDateWhen {
            val repoMarker =
                configuration.resolvedConfiguration.resolvedArtifacts.single().moduleDirectory().resolve(".marker")
            repoMarker.exists()
        }

        doFirst {
            val artifact = configuration.resolvedConfiguration.resolvedArtifacts.single()
            val moduleDirectory = artifact.moduleDirectory()

            artifact.storeDirectory().cleanStore()

            val repoMarker = File(moduleDirectory, ".marker")
            if (repoMarker.exists()) {
                logger.info("Path ${repoMarker.absolutePath} already exists, skipping unpacking.")
                return@doFirst
            }

            with(artifact) {
                val artifactsDirectory = File(moduleDirectory, "artifacts")
                logger.info("Unpacking ${file.name} into ${artifactsDirectory.absolutePath}")
                copy {
                    val fileTree = when (extension) {
                        "tar.gz" -> tarTree(file)
                        "zip" -> zipTree(file)
                        else -> error("Unsupported artifact extension: $extension")
                    }

                    from(
                        fileTree.matching {
                            include(
                                "build.txt",
                                "lib/*.jar",
                                "plugins/java/lib/*.jar",
                                "plugins/Kotlin/lib/*.jar",
                                "plugins/properties/lib/*.jar"
                            )
                        }
                    )

                    into(artifactsDirectory)
                    includeEmptyDirs = false
                }

                ClassFileTransformer.transformFilesInDir(file(File(artifactsDirectory, "lib")), logger)

                writeIvyXml(
                    organization,
                    moduleVersion.id.name,
                    moduleVersion.id.version,
                    moduleVersion.id.name,
                    File(artifactsDirectory, "lib"),
                    File(artifactsDirectory, "lib"),
                    File(moduleDirectory, "ivy"),
                    *listOfNotNull(sources).toTypedArray()
                )

                val pluginsDirectory = File(artifactsDirectory, "plugins")
                if (pluginsDirectory.exists()) {
                    file(File(artifactsDirectory, "plugins"))
                        .listFiles { file: File -> file.isDirectory }
                        .forEach {
                            ClassFileTransformer.transformFilesInDir(file(File(it, "lib")), logger)

                            writeIvyXml(
                                organization,
                                it.name,
                                moduleVersion.id.version,
                                it.name,
                                File(it, "lib"),
                                File(it, "lib"),
                                File(moduleDirectory, "ivy"),
                                *listOfNotNull(sources).toTypedArray()
                            )
                        }
                }

                repoMarker.createNewFile()
            }
        }
    }
}

fun CleanableStore.cleanStore() = cleanDir(Instant.now().minus(Duration.ofDays(30)))

fun writeIvyXml(
    organization: String,
    moduleName: String,
    version: String,
    fileName: String,
    baseDir: File,
    artifactDir: File,
    targetDir: File,
    vararg sourcesJar: File
) {
    fun shouldIncludeIntellijJar(jar: File) = jar.isFile && jar.extension == "jar"

    val ivyFile = targetDir.resolve("$fileName.ivy.xml")
    ivyFile.parentFile.mkdirs()
    with(XMLWriter(ivyFile.writer())) {
        document("UTF-8", "1.0") {
            element("ivy-module") {
                attribute("version", "2.0")
                attribute("xmlns:m", "http://ant.apache.org/ivy/maven")

                emptyElement("info") {
                    attributes(
                        "organisation" to organization,
                        "module" to moduleName,
                        "revision" to version,
                        "publication" to SimpleDateFormat("yyyyMMddHHmmss").format(Date())
                    )
                }

                element("configurations") {
                    listOf("default", "sources").forEach { configurationName ->
                        emptyElement("conf") {
                            attributes("name" to configurationName, "visibility" to "public")
                        }
                    }
                }

                element("publications") {
                    artifactDir.listFiles()
                        ?.filter(::shouldIncludeIntellijJar)
                        ?.sortedBy { it.name.toLowerCase() }
                        ?.forEach { jarFile ->
                            val relativeName = jarFile.toRelativeString(baseDir).removeSuffix(".jar")
                            emptyElement("artifact") {
                                attributes(
                                    "name" to relativeName,
                                    "type" to "jar",
                                    "ext" to "jar",
                                    "conf" to "default"
                                )
                            }
                        }

                    sourcesJar.forEach { jarFile ->
                        emptyElement("artifact") {
                            val sourcesArtifactName = jarFile.name.substringBefore("-$version")
                            attributes(
                                "name" to sourcesArtifactName,
                                "type" to "jar",
                                "ext" to "jar",
                                "conf" to "sources",
                                "m:classifier" to "sources"
                            )
                        }
                    }
                }
            }
        }

        close()
    }
}

class XMLWriter(private val outputStreamWriter: OutputStreamWriter) : Closeable {

    private val xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStreamWriter)

    private var depth = 0
    private val indent = "  "

    fun document(encoding: String, version: String, init: XMLWriter.() -> Unit) = apply {
        xmlStreamWriter.writeStartDocument(encoding, version)
        init()
        xmlStreamWriter.writeEndDocument()
    }

    fun element(name: String, init: XMLWriter.() -> Unit) = apply {
        writeIndent()
        xmlStreamWriter.writeStartElement(name)
        depth += 1
        init()
        depth -= 1
        writeIndent()
        xmlStreamWriter.writeEndElement()
    }

    fun emptyElement(name: String, init: XMLWriter.() -> Unit) = apply {
        writeIndent()
        xmlStreamWriter.writeEmptyElement(name)
        init()
    }

    fun attribute(name: String, value: String): Unit = xmlStreamWriter.writeAttribute(name, value)

    fun attributes(vararg attributes: Pair<String, String>) {
        attributes.forEach { attribute(it.first, it.second) }
    }

    private fun writeIndent() {
        xmlStreamWriter.writeCharacters("\n")
        repeat(depth) {
            xmlStreamWriter.writeCharacters(indent)
        }
    }

    override fun close() {
        xmlStreamWriter.flush()
        xmlStreamWriter.close()
        outputStreamWriter.close()
    }
}

internal object ClassFileTransformer {

    private val processingPackagesPrefixes = listOf(
        "com/intellij/",
        "org/intellij/plugins/",
        "com/android/tools/idea/",
        "org/jetbrains/android/download/",
        "org/jetbrains/idea/devkit/testAssistant/",
        "org/jetbrains/plugins/"
    )

    private val classMapping = mapOf(
        "com/intellij/ide/IdeEventQueue"
            to "com/intellij/ide/IpwEventQueue",

        "com/intellij/ide/IdeEventQueue\$EventDispatcher"
            to "com/intellij/ide/IpwEventQueue\$EventDispatcher",

        "com/intellij/ide/IdeEventQueue\$PostEventHook"
            to "com/intellij/ide/IpwEventQueue\$PostEventHook",

        "com/intellij/openapi/progress/util/ProgressWindow"
            to "com/intellij/openapi/progress/util/IpwProgressWindow",

        "com/intellij/openapi/progress/util/PotemkinProgress"
            to "com/intellij/openapi/progress/util/IpwPotemkinProgress",

        "com/intellij/openapi/editor/impl/EditorImpl"
            to "com/intellij/openapi/editor/impl/IpwEditorImpl",

        "com/intellij/openapi/editor/impl/EditorImpl\$MyScrollBar"
            to "com/intellij/openapi/editor/impl/IpwEditorImpl\$MyScrollBar",

        "com/intellij/codeInsight/daemon/impl/StatusBarUpdater"
            to "com/intellij/codeInsight/daemon/impl/IpwStatusBarUpdater",

        "com/intellij/codeInsight/lookup/impl/LookupImpl"
            to "com/intellij/codeInsight/lookup/impl/IpwLookupImpl"
    )

    private const val applicationClass = "com/intellij/openapi/application/impl/ApplicationImpl"

    fun transformFilesInDir(dir: File, logger: Logger) {
        if (dir.isDirectory) {
            dir.listFiles { file -> file.isFile && file.extension == "jar" }
                .forEach { transformJarFile(it, logger) }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun transformJarFile(jarFile: File, logger: Logger) {
        val env = hashMapOf("create" to "false")
        FileSystemProvider.installedProviders().filter { it.scheme == "jar" }.mapNotNull {
            it.newFileSystem(jarFile.toPath(), env)
        }.first().use {
            // pass the logger as a parameter to workaround the error at this point:
            // "Back-end (JVM) Internal error: Failed to generate expression: KtNameReferenceExpression"
            logger.info("Transforming classes from $jarFile")
            val root = it.getPath("/")
            Files.walkFileTree(root, object : SimpleFileVisitor<java.nio.file.Path>() {
                override fun visitFile(classFile: java.nio.file.Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    if (classFile != null && classFile.extension == "class") {
                        val classFilePath = classFile.toString()
                        val className = classFilePath.substring(1, classFilePath.lastIndexOf('.'))
                        if (startsWithProcessingPackagePrefix(className)) {
                            val initialBytes = Files.readAllBytes(classFile)
                            logger.info("transforming $className")
                            val resultBytes = transformClass(className, initialBytes)
                            if (Arrays.hashCode(initialBytes) != Arrays.hashCode(resultBytes)) {
                                logger.info("rewriting $className")
                                Files.write(classFile, resultBytes)
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }

    private fun startsWithProcessingPackagePrefix(className: String): Boolean =
        processingPackagesPrefixes.any(className::startsWith)

    private fun transformClass(className: String, classByteArray: ByteArray): ByteArray {
        val cr = ClassReader(classByteArray)
        val cw = ClassWriter(cr, 0)
        val remapper = ClassRemapper(cw, SimpleRemapper(classMapping))

        cr.accept(
            EdtInvocationMapperCV(remapper, EdtInvocationMapper(), applicationClass == className),
            0
        )

        return cw.toByteArray()
    }
}

internal class EdtInvocationMapper {

    private val adapterClass = "org/jetbrains/compose/codeeditor/platform/impl/edt/EdtAdapter"

    private val classMethodMapping = mapOf(
        "javax/swing/SwingUtilities" to mapOf(
            "isEventDispatchThread" to "isEventDispatchThread",
            "invokeLater" to "invokeLater",
            "invokeAndWait" to "invokeAndWait"
        ),
        "java/awt/EventQueue" to mapOf(
            "isDispatchThread" to "isEventDispatchThread",
            "invokeLater" to "invokeLater",
            "invokeAndWait" to "invokeAndWait"
        ),
        "com/intellij/util/ui/EdtInvocationManager" to mapOf(
            "dispatchAllInvocationEvents" to "dispatchAllInvocationEvents",
            "getEventQueueThread" to "getEventQueueThread",
            "invokeLaterIfNeeded" to "invokeLaterIfNeeded",
            "invokeAndWaitIfNeeded" to "invokeAndWaitIfNeeded"
        )
    )

    fun mapType(typeName: String, methodName: String): String {
        val methodMap = classMethodMapping[typeName]
        return if (methodMap != null && methodMap.containsKey(methodName)) {
            adapterClass
        } else typeName
    }

    fun mapMethodName(owner: String, name: String): String {
        val methodMap = classMethodMapping[owner]
        return methodMap?.getOrDefault(name, name) ?: name
    }

    fun mapValue(value: Any): Any {
        when (value) {

            is Handle -> {
                val methodMap = classMethodMapping[value.owner]
                if (methodMap != null) {
                    val methodName = methodMap[value.name]
                    if (methodName != null) {
                        return Handle(
                            value.tag,
                            adapterClass,
                            methodName,
                            value.desc,
                            value.isInterface
                        )
                    }
                }
                return value
            }

            is ConstantDynamic -> {
                val bootstrapMethodArgumentCount = value.bootstrapMethodArgumentCount
                val remappedBootstrapMethodArguments = arrayOfNulls<Any>(bootstrapMethodArgumentCount)
                for (i in 0 until bootstrapMethodArgumentCount) {
                    remappedBootstrapMethodArguments[i] = mapValue(value.getBootstrapMethodArgument(i))
                }
                val descriptor = value.descriptor
                return ConstantDynamic(
                    value.name,
                    descriptor,
                    mapValue(value.bootstrapMethod) as Handle,
                    *remappedBootstrapMethodArguments)
            }

            else -> return value
        }
    }
}

internal class EdtInvocationMapperMV(mv: MethodVisitor?, mapper: EdtInvocationMapper, isApplicationClass: Boolean) :
    MethodVisitor(Opcodes.ASM9, mv) {

    companion object {
        private const val AWTClass = "sun/awt/AWTAutoShutdown"
    }

    private val mapper: EdtInvocationMapper
    private val isApplicationClass: Boolean
    private var AWTAutoShutDown = false

    init {
        this.mapper = mapper
        this.isApplicationClass = isApplicationClass
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
        if (isApplicationClass && owner == AWTClass) {
            AWTAutoShutDown = !AWTAutoShutDown
            return
        }
        mv.visitMethodInsn(
            opcode,
            mapper.mapType(owner, name),
            mapper.mapMethodName(owner, name),
            descriptor,
            isInterface)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        if (!AWTAutoShutDown) mv.visitVarInsn(opcode, `var`)
    }

    override fun visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle,
                                        vararg bootstrapMethodArguments: Any) {
        val len = bootstrapMethodArguments.size
        val remappedBootstrapMethodArguments = arrayOfNulls<Any>(len)
        for (i in 0 until len) {
            remappedBootstrapMethodArguments[i] = mapper.mapValue(bootstrapMethodArguments[i])
        }
        mv.visitInvokeDynamicInsn(
            name,
            descriptor,
            mapper.mapValue(bootstrapMethodHandle) as Handle,
            *remappedBootstrapMethodArguments)
    }
}

internal class EdtInvocationMapperCV(cv: ClassVisitor?, mapper: EdtInvocationMapper, isApplicationClass: Boolean) :
    ClassVisitor(Opcodes.ASM9, cv) {

    private val mapper: EdtInvocationMapper
    private val isApplicationClass: Boolean

    init {
        this.mapper = mapper
        this.isApplicationClass = isApplicationClass
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?,
                             exceptions: Array<String>?): MethodVisitor {
        var mv = cv.visitMethod(access, name, descriptor, signature, exceptions)
        if (mv != null) {
            mv = EdtInvocationMapperMV(mv, mapper, isApplicationClass)
        }
        return mv
    }
}
