@file:OptIn(
  org.jetbrains.compose.resources.InternalResourceApi::class,
  org.jetbrains.compose.resources.ExperimentalResourceApi::class,
)

package my.lib.res

import kotlin.ByteArray
import kotlin.OptIn
import kotlin.String
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

@ExperimentalResourceApi
public object Res {
  /**
   * Reads the content of the resource file at the specified path and returns it as a byte array.
   *
   * Example: `val bytes = Res.readBytes("files/key.bin")`
   *
   * @param path The path of the file to read in the compose resource's directory.
   * @return The content of the file as a byte array.
   */
  public suspend fun readBytes(path: String): ByteArray = readResourceBytes("" + path)

  public object drawable

  public object string

  public object font
}
