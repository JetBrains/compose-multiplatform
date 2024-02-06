@file:OptIn(
    org.jetbrains.compose.resources.InternalResourceApi::class,
    org.jetbrains.compose.resources.ExperimentalResourceApi::class,
)

package app.group.empty_res.generated.resources

import kotlin.ByteArray
import kotlin.OptIn
import kotlin.String
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getResourceUriString
import org.jetbrains.compose.resources.readResourceBytes

@ExperimentalResourceApi
internal object Res {
    /**
     * Reads the content of the resource file at the specified path and returns it as a byte array.
     *
     * Example: `val bytes = Res.readBytes("files/key.bin")`
     *
     * @param path The path of the file to read in the compose resource's directory.
     * @return The content of the file as a byte array.
     */
    public suspend fun readBytes(path: String): ByteArray = readResourceBytes(path)

    /**
     * Returns the URI string of the resource file at the specified path.
     *
     * Example: `val uri = Res.getUri("files/key.bin")`
     *
     * @param path The path of the file to get the URI string from in the compose resource's
     * directory.
     * @return The URI string of the file.
     */
    public fun getUri(path: String): String = getResourceUriString(path)
}