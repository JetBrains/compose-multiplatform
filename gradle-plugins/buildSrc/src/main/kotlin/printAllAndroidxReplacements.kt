import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project

private const val libsRepo = "https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/"
private val exceptions = listOf(
    "desktop:desktop",
    "compose-full",
    "compose-gradle-plugin"
)

fun Project.printAllAndroidxReplacements() = runBlocking {
    val version = BuildProperties.composeVersion(project)
    HttpClient().use { client ->
        client
            .allRecursiveFolders(libsRepo)
            .map { it.removePrefix(libsRepo).removeSuffix("/") }
            .filter { it.endsWith(version) }
            .map { it.removeSuffix(version).removeSuffix("/") }
            .map { it.replace("/", ":") }
            .filter { !it.endsWith("-android") }
            .filter { !it.endsWith("-android-debug") }
            .filter { !it.endsWith("-android-release") }
            .filter { !it.endsWith("-metadata") }
            .filter { !it.endsWith("-desktop") }
            .filter { !it.contains("-jvm") }
            .filter { !exceptions.contains(it) }
            .collect {
                require(isMavenCoordsValid(it)) {
                    "module name isn't valid: $it"
                }
                println("it.replaceAndroidx(\"androidx.compose.$it\", \"org.jetbrains.compose.$it\")")
            }
    }
}

private fun isMavenCoordsValid(coords: String) = coords.count { it == ':' } == 1

private fun HttpClient.allRecursiveFolders(
    url: String
): Flow<String> = flow {
    require(url.endsWith("/"))
    val response = get<String>(url)
    val folders = parseFolders(response)
    for (folder in folders) {
        emit("$url$folder/")
    }
    for (folder in folders.filter(String::isMavenPart)) {
        allRecursiveFolders("$url$folder/").collect(::emit)
    }
}

private fun parseFolders(
    htmlResponse: String
): Sequence<String> = Regex("title=\"(.*?)\"")
    .findAll(htmlResponse)
    .map { it.groupValues[1] }
    .filter { it.endsWith("/") && it != "../" }
    .map { it.removeSuffix("/") }

private fun String.isMavenPart() = all { it.isLetterOrDigit() || it == '-' }