package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
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

    @TaskAction
    fun UpdateResourceApi() {
        if (oldApiFile == null || !!oldApiFile!!.exists()) {
            if (newApiFile != null && newApiFile!!.exists()) {
                newApiFile?.copyTo(File(project.projectDir,
                    "api/res-${project.version}.txt"), true, 8)
                return
            } else {
                File(project.projectDir, "api/res-${project.version}.txt").createNewFile()
                return
            }
        }
        var oldResourceApi: HashSet<String> = HashSet<String>(oldApiFile?.readLines())
        var newResourceApi: HashSet<String> = HashSet<String>()
        if (newApiFile != null && newApiFile!!.exists()) {
            newResourceApi = HashSet<String>(newApiFile?.readLines())
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
        if (oldVersion.major == project.version().major && !removedResourceApi.isEmpty()) {
            var errorMessage = "Cannot remove public resources within the same major version, " +
                    "the following were removed since version $oldVersion:\n"
            for (e in oldResourceApi) {
                errorMessage = errorMessage + "$e\n"
            }
            throw GradleException(errorMessage)
        }
        if (oldVersion.major == project.version().major &&
                oldVersion.minor == project.version().minor && !addedResourceApi.isEmpty() &&
                project.version().isFinalApi()) {
            var errorMessage = "Cannot add public resources when api becomes final, " +
                    "the following resources were added since version $oldVersion:\n"
            for (e in newResourceApi) {
                errorMessage = errorMessage + "$e\n"
            }
            throw GradleException(errorMessage)
        }
        newResourceApi.addAll(newResourceApi)
        val sortedNewResourceApi: SortedSet<String> = newResourceApi.toSortedSet()
        File(project.projectDir, "api/res-${project.version}.txt").bufferedWriter().use { out ->
            sortedNewResourceApi.forEach {
                out.write(it)
                out.newLine()
            }
        }
    }
}
