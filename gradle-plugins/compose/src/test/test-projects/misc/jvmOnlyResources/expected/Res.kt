package me.app.jvmonlyresources.generated.resources

import kotlin.ByteArray
import kotlin.OptIn
import kotlin.String
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.readResourceBytes

@OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)
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

    public object drawable {
        public val vector: DrawableResource = DrawableResource(
            "drawable:vector",
            setOf(
                ResourceItem(
                    setOf(),
                    "drawable/vector.xml"
                ),
            )
        )
    }
}