@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import org.jetbrains.compose.resources.StringResource

private object DesktopMainString0 {
    public val desktop_str: StringResource by
    lazy { init_desktop_str() }
}

public val Res.string.desktop_str: StringResource
    get() = DesktopMainString0.desktop_str

private fun init_desktop_str(): StringResource = org.jetbrains.compose.resources.StringResource(
    "string:desktop_str", "desktop_str",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(),
            "composeResources/my.lib.res/values/desktop_strings.desktopMain.cvr", 10, 39),
    )
)