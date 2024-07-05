@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource

private object DesktopMainString0 {
  public val desktop_str: StringResource by 
      lazy { init_desktop_str() }
}

@InternalResourceApi
internal fun _collectDesktopMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("desktop_str", DesktopMainString0.desktop_str)
}

internal val Res.string.desktop_str: StringResource
  get() = DesktopMainString0.desktop_str

private fun init_desktop_str(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:desktop_str", "desktop_str",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/app.group.resources_test.generated.resources/values/desktop_strings.desktopMain.cvr",
    10, 39),
    )
)
