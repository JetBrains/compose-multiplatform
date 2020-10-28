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
