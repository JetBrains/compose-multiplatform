// READ ME FIRST!
//
// Code in this file is shared between the Android and Desktop JVM targets.
// Kotlin's hierarchical multiplatform projects currently
// don't support sharing code depending on JVM declarations.
//
// You can follow the progress for HMPP JVM & Android intermediate source sets here:
// https://youtrack.jetbrains.com/issue/KT-42466
//
// The workaround used here to access JVM libraries causes IntelliJ IDEA to not
// resolve symbols in this file properly.
//
// Resolution errors in your IDE do not indicate a problem with your setup.


package example.imageviewer.model

import example.imageviewer.core.Repository
import example.imageviewer.utils.ktorHttpClient
import example.imageviewer.utils.runBlocking
import io.ktor.client.request.*

class ImageRepository(
    private val httpsURL: String
) : Repository<MutableList<String>> {

    override fun get(): MutableList<String> {
        return runBlocking {
            val content = ktorHttpClient.get<String>(httpsURL)
            content.lines().toMutableList()
        }
    }
}
