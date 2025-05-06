@file:OptIn(InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource

private const val MD: String = "composeResources/app.group.resources_test.generated.resources/"

internal val Res.string.desktop_str: StringResource by lazy {
      StringResource("string:desktop_str", "desktop_str", setOf(
        ResourceItem(setOf(), "${MD}values/desktop_strings.desktopMain.cvr", 10, 39),
      ))
    }

@InternalResourceApi
internal fun _collectDesktopMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("desktop_str", Res.string.desktop_str)
}
