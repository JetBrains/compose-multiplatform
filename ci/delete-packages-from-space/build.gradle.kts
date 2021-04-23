import io.ktor.client.*
import io.ktor.client.engine.jetty.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FilenameUtils
import space.jetbrains.api.runtime.*
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.*
import java.util.*
import kotlin.collections.ArrayList

val checkJavaVersion = tasks.register("checkJavaVersion") {
    doLast {
        check(JavaVersion.current() >= JavaVersion.VERSION_1_9) {
            "Use JDK 9+ to run this task"
        }
    }
}

tasks.register("listProjectsAndPackageRepositories") {
    dependsOn(checkJavaVersion)

    doLast {
        Space().listProjectsAndPackageRepositories()
    }
}

val packagesToDeleteFile: File
    get() = project.buildDir.resolve("packages-to-delete.txt")

tasks.register("generateListOfPackagesToDelete") {
    dependsOn(checkJavaVersion)
    doLast {
        Space().preparePackagesToDelete(packagesToDeleteFile)
    }
}

tasks.register("deletePackages") {
    dependsOn(checkJavaVersion)
    doLast {
        Space().deletePackages(packagesToDeleteFile)
    }
}

fun getLocalProperties() =
    Properties().apply {
        val file = project.file("local.properties")
        if (file.exists()) {
            file.inputStream().buffered().use { input ->
                load(input)
            }
        }
    }

class Space {
    private val localProperties = getLocalProperties()

    fun property(name: String): String =
        (localProperties.getProperty(name))
            ?: (project.findProperty(name) as? String)
            ?: error("Property '$name' is not set")


    val token by lazy { property("space.auth.token") }
    val server by lazy { property("space.server.url") }
    val projectId by lazy { ProjectIdentifier.Id(property("space.project.id")) }
    val repoId by lazy { PackageRepositoryIdentifier.Id(property("space.repo.id")) }
    val packageVersionToDelete by lazy { property("space.package.version") }

    fun withSpaceClient(fn: suspend SpaceHttpClientWithCallContext.() -> Unit) {
        runBlocking<Unit> {
            HttpClient(Jetty).use { client ->
                val space = SpaceHttpClient(client).withPermanentToken(
                    token = token,
                    serverUrl = server
                )
                space.fn()
            }
        }
    }

    fun batches(batchSize: Int = 100) =
        generateSequence(0) { it + batchSize }
            .map { BatchInfo(it.toString(), batchSize) }

    suspend fun <T> forAllInAllBatches(
        getBatch: suspend (BatchInfo) -> Batch<T>,
        fn: suspend (T) -> Unit
    ) {
        for (batchInfo in batches()) {
            val batch = getBatch(batchInfo)

            for (element in batch.data) {
                fn(element)
            }

            if (batch.data.isEmpty() || (batch.next.toIntOrNull() ?: 0) >= (batch.totalCount ?: 0)) return
        }
    }

    suspend fun SpaceHttpClientWithCallContext.forEachPackage(fn: suspend (PackageData) -> Unit) {
        forAllInAllBatches({ batch ->
            projects.packages.repositories.packages.getAllPackages(
                project = projectId,
                repository = repoId,
                query = "",
                batchInfo = batch
            )
        }, fn)
    }

    suspend fun SpaceHttpClientWithCallContext.forEachVersion(packageName: String, fn: suspend (String) -> Unit) {
        forAllInAllBatches({ batch ->
            projects.packages.repositories.packages.versions.getAllPackageVersions(
                projectId, repoId,
                packageName,
                query = "",
                sortColumn = PackagesSortColumn.Created,
                sortOrder = ColumnSortOrder.DESC,
                batchInfo = batch
            )
        }) { fn(it.version) }
    }

    suspend fun SpaceHttpClientWithCallContext.forEachProject(fn: suspend (PR_Project) -> Unit) {
        forAllInAllBatches({ batch ->
            projects.getAllProjects(batchInfo = batch)
        }, fn)
    }

    suspend fun SpaceHttpClientWithCallContext.forEachPackageRepository(
        proj: PR_Project,
        fn: (ProjectPackageRepository) -> Unit
    ) {
        projects.packages.repositories.getRepositories(
            ProjectIdentifier.Id(proj.id)
        ).forEach(fn)
    }
}

fun Space.listProjectsAndPackageRepositories() {
    withSpaceClient {
        forEachProject { proj ->
            logger.quiet("Project '${proj.name}'(id: ${proj.id})")
            forEachPackageRepository(proj) { repo ->
                logger.quiet("    Package repository '${repo.name}(id: ${repo.id})'")
            }
        }
    }
}

fun Space.preparePackagesToDelete(packagesFile: File) {
    logger.quiet("Preparing list of packages for deletion. This may take some time...")

    val packagesToDelete = ArrayList<PackageInfo>()
    withSpaceClient {
        forEachPackage { pkg ->
            forEachVersion(pkg.name) { version ->
                if (FilenameUtils.wildcardMatch(version, packageVersionToDelete)) {
                    packagesToDelete.add(PackageInfo(name = pkg.name, version = version))
                }
            }
            logger.quiet("Analyzed package: ${pkg.name}")
        }
    }

    packagesFile.parentFile.mkdirs()
    packagesFile.writer().buffered().use { writer ->
        packagesToDelete.forEach {
            writer.write("#")
            writer.write(it.name)
            writer.write(":")
            writer.write(it.version)
            writer.newLine()
        }
    }

    logger.quiet("List of packages to delete is written to:\n    $packagesFile")
    logger.quiet("Uncomment packages you want to delete and rerun the task!")
}

fun Space.deletePackages(packagesFile: File) {
    if (!packagesFile.exists()) {
        error("A list of packages to delete does not exist, run 'generateListOfPackagesToDelete' first")
    }
    val packagesToDelete = ArrayList<PackageInfo>()
    packagesFile.forEachLine { line ->
        if (!line.startsWith("#")) {
            val split = line.split(":")
            if (split.size == 2) {
                packagesToDelete.add(PackageInfo(name = split[0], version = split[1]))
            }
        }
    }

    if (packagesToDelete.isEmpty()) {
        logger.quiet("No packages to delete!")
        logger.quiet("Uncomment packages to delete them: ${packagesFile}")
    } else {
        val allPackagesToBeDeletedText = packagesToDelete.joinToString("\n") { "${it.name}:${it.version}" }
        if (ConfirmDeletionDialog.confirm(allPackagesToBeDeletedText)) {
            logger.quiet("Deleting ${packagesToDelete.size} packages...")
            withSpaceClient {
                for (pkg in packagesToDelete) {
                    projects.packages.repositories.packages.versions.deletePackageVersion(
                        projectId, repoId, packageName = pkg.name, packageVersion = pkg.version
                    )
                    logger.quiet("Deleted package: ${pkg.name}:${pkg.version}")
                }
            }
            packagesFile.copyTo(packagesFile.resolveSibling(packagesFile.nameWithoutExtension + ".deleted.txt"))
            packagesFile.delete()
        }
    }
}

class PackageInfo(
    val name: String,
    val version: String
)