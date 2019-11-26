package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.SortedSet

/**
 * Task for updating the public resource surface
 */
open class UpdateResourceApiTask : DefaultTask() {

    @InputFiles
    @Optional
    var oldApiFile: File? = null

    @InputFiles
    @Optional
    var newApiFile: File? = null

    @OutputFile
    var destApiFile: File? = null

    @TaskAction
    fun UpdateResourceApi() {
        val destApiFile = checkNotNull(destApiFile) { "destApiFile not set" }
        if (oldApiFile == null || !oldApiFile!!.exists()) {
            if (newApiFile != null && newApiFile!!.exists()) {
                newApiFile?.copyTo(destApiFile, true, 8)
                return
            } else {
                destApiFile.createNewFile()
                return
            }
        }
        val oldResourceApi: HashSet<String> = HashSet(oldApiFile?.readLines())
        var newResourceApi: HashSet<String> = HashSet()
        if (newApiFile != null && newApiFile!!.exists()) {
            newResourceApi = HashSet(newApiFile?.readLines())
        }
        val removedResourceApi = HashSet<String>()
        val addedResourceApi = HashSet<String>(newResourceApi)
        for (e in oldResourceApi) {
            if (newResourceApi.contains(e)) {
                addedResourceApi.remove(e)
            } else {
                removedResourceApi.add(e)
            }
        }
        val oldVersion = Version(oldApiFile!!.name.removePrefix("res-").removeSuffix(".txt"))
        if (oldVersion.major == project.version().major && removedResourceApi.isNotEmpty()) {
            var errorMessage = "Cannot remove public resources within the same major version, " +
                    "the following were removed since version $oldVersion:\n"
            for (e in removedResourceApi) {
                errorMessage += "$e\n"
            }
            throw GradleException(errorMessage)
        }
        if (oldVersion.major == project.version().major &&
                oldVersion.minor == project.version().minor && addedResourceApi.isNotEmpty() &&
                project.version().isFinalApi()) {
            var errorMessage = "Cannot add public resources when api becomes final, " +
                    "the following resources were added since version $oldVersion:\n"
            for (e in addedResourceApi) {
                errorMessage += "$e\n"
            }
            throw GradleException(errorMessage)
        }
        newResourceApi.addAll(newResourceApi)
        val sortedNewResourceApi: SortedSet<String> = newResourceApi.toSortedSet()
        destApiFile.bufferedWriter().use { out ->
            sortedNewResourceApi.forEach {
                out.write(it)
                out.newLine()
            }
        }
    }
}
