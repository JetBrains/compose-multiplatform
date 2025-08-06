@file:OptIn(InternalResourceApi::class)
@file:Suppress(
  "RedundantVisibilityModifier",
  "REDUNDANT_VISIBILITY_MODIFIER",
)

package app.group.resources_test.generated.resources

import kotlin.ByteArray
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import org.jetbrains.compose.resources.InternalResourceApi
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
  public suspend fun readBytes(path: String): ByteArray = readResourceBytes("composeResources/app.group.resources_test.generated.resources/" + path)

  /**
   * Returns the URI string of the resource file at the specified path.
   *
   * Example: `val uri = Res.getUri("files/key.bin")`
   *
   * @param path The path of the file in the compose resource's directory.
   * @return The URI string of the file.
   */
  public fun getUri(path: String): String = getResourceUri("composeResources/app.group.resources_test.generated.resources/" + path)

  public object drawable

  public object string

  public object array

  public object plurals

  public object font
}
