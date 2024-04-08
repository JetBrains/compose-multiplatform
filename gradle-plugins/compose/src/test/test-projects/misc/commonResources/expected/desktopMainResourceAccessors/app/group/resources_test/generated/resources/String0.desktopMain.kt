@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource

@ExperimentalResourceApi
private object DesktopMainString0 {
    public val desktop_str: StringResource by
    lazy { init_desktop_str() }
}

@ExperimentalResourceApi
internal val Res.string.desktop_str: StringResource
    get() = DesktopMainString0.desktop_str

@ExperimentalResourceApi
private fun init_desktop_str(): StringResource = org.jetbrains.compose.resources.StringResource(
    "string:desktop_str", "desktop_str",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "values/desktop_strings.cvr", 10, 39),
    )
)