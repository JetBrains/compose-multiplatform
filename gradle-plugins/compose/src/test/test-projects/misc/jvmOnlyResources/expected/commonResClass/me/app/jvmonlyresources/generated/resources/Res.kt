@file:OptIn(
    org.jetbrains.compose.resources.InternalResourceApi::class,
    org.jetbrains.compose.resources.ExperimentalResourceApi::class,
)

package me.app.jvmonlyresources.generated.resources

import kotlin.ByteArray
import kotlin.OptIn
import kotlin.String
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getResourceUri
import org.jetbrains.compose.resources.readResourceBytes

internal object Res {
    /**
     * Reads the content of the resource file at the specified path and returns it as a byte array.
     *
     * Example: `val bytes = Res.readBytes("files/key.bin")`
     *
     * @param path The path of the file to read in the compose resource's directory.
     * @return The content of the file as a byte array.
     */
    @ExperimentalResourceApi
    public suspend fun readBytes(path: String): ByteArray = readResourceBytes("" + path)

    /**
     * Returns the URI string of the resource file at the specified path.
     *
     * Example: `val uri = Res.getUri("files/key.bin")`
     *
     * @param path The path of the file in the compose resource's directory.
     * @return The URI string of the file.
     */
    @ExperimentalResourceApi
    public fun getUri(path: String): String = getResourceUri("" + path)

    public object drawable {
        /**
         * Returns the resource accessor by the specified path.
         *
         * NOTE: if the file does not match the resource type, there will be a crash in runtime!
         *
         * @param path The path of the file in the compose resource's directory.
         * @return The accessor to the specified file.
         */
        @ExperimentalResourceApi
        public fun byPath(path: String): DrawableResource =
            org.jetbrains.compose.resources.DrawableResource(
                "drawable:" + path,
                setOf(org.jetbrains.compose.resources.ResourceItem(setOf(), "" + path, -1, -1))
            )
    }

    public object string

    public object array

    public object plurals

    public object font {
        /**
         * Returns the resource accessor by the specified path.
         *
         * NOTE: if the file does not match the resource type, there will be a crash in runtime!
         *
         * @param path The path of the file in the compose resource's directory.
         * @return The accessor to the specified file.
         */
        @ExperimentalResourceApi
        public fun byPath(path: String): FontResource = org.jetbrains.compose.resources.FontResource(
            "font:" + path,
            setOf(org.jetbrains.compose.resources.ResourceItem(setOf(), "" + path, -1, -1))
        )
    }
}