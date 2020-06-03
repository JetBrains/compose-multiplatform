package androidx.build

import androidx.build.checkapi.ApiLocation
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.SortedSet

/**
 * Task for updating the public resource surface
 */
abstract class UpdateResourceApiTask : DefaultTask() {
    /** Optional text file from which the previously-released resource signatures will be read. */
    @get:InputFile
    @get:Optional
    abstract val referenceResourceApiFile: Property<File>

    /**
     * Text file from which resource signatures will be read. A file path must be specified at
     * configuration time even if the file may not exist at build time.
     */
    @get:Internal
    abstract val inputApiFile: Property<File>

    @InputFile
    @Optional
    fun getInputApiFileIfExists(): File? {
        val file = inputApiFile.get()
        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    /** Text files to which resource signatures will be written. */
    @get:Internal
    abstract val outputApiLocations: ListProperty<ApiLocation>

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return outputApiLocations.get().flatMap { outputApiLocation ->
            listOf(
                outputApiLocation.resourceFile
            )
        }
    }

    @TaskAction
    fun verifyAndUpdateResourceApi() {
        val newApiFile = inputApiFile.get()
        val referenceApiFile = referenceResourceApiFile.orNull

        // Read the current API surface, if any, into memory.
        val newApiSet = if (newApiFile.exists()) {
            HashSet(newApiFile.readLines())
        } else {
            emptySet<String>()
        }

        // If a reference API file was specified, verify the current API surface.
        if (referenceApiFile != null && referenceApiFile.exists()) {
            // Read the reference API surface into memory.
            val oldVersion = Version(
                referenceApiFile.name.removePrefix("res-").removeSuffix(".txt")
            )
            val oldApiSet: HashSet<String> = HashSet(referenceApiFile.readLines())
            checkApiCompatibility(oldVersion, oldApiSet, project.version(), newApiSet)
        }

        // Sort the resources for the sake of source control diffs.
        val newApiSortedSet: SortedSet<String> = newApiSet.toSortedSet()

        // Write current API surface to output locations.
        for (outputApiLocation in outputApiLocations.get()) {
            val outputApiFile = outputApiLocation.resourceFile
            outputApiFile.bufferedWriter().use { out ->
                newApiSortedSet.forEach {
                    out.write(it)
                    out.newLine()
                }
            }
        }
    }

    private fun checkApiCompatibility(
        referenceVersion: Version,
        referenceApiSet: Set<String>,
        newVersion: Version,
        newApiSet: Set<String>
    ) {
        // Compute the diff.
        val removedApi = HashSet<String>()
        val addedApi = HashSet<String>(newApiSet)
        for (e in referenceApiSet) {
            if (newApiSet.contains(e)) {
                addedApi.remove(e)
            } else {
                removedApi.add(e)
            }
        }

        // POLICY: Ensure that no resources are removed within the span of a major version.
        if (referenceVersion.major == newVersion.major && removedApi.isNotEmpty()) {
            var errorMessage = "Cannot remove public resources within the same major version, " +
                    "the following were removed since version $referenceVersion:\n"
            for (e in removedApi) {
                errorMessage += "$e\n"
            }
            throw GradleException(errorMessage)
        }

        // POLICY: Ensure that no resources are added to a finalized version.
        if (newVersion.isFinalApi() && addedApi.isNotEmpty()) {
            var errorMessage = "Cannot add public resources when api becomes final, " +
                    "the following resources were added since version $referenceVersion:\n"
            for (e in addedApi) {
                errorMessage += "$e\n"
            }
            throw GradleException(errorMessage)
        }
    }
}
