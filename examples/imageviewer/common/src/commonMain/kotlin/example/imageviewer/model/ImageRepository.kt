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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ImageRepository(
    private val httpsURL: String
) : Repository<MutableList<String>> {

    override fun get(): MutableList<String> {
        val list: MutableList<String> = ArrayList()
        try {
            val url = URL(httpsURL)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.connect()

            val read = BufferedReader(InputStreamReader(connection.inputStream))

            var line: String? = read.readLine()
            while (line != null) {
                list.add(line)
                line = read.readLine()
            }
            read.close()
            return list
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }
}
