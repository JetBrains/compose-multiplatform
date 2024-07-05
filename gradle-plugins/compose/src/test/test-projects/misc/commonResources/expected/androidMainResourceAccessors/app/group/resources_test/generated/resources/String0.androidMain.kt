@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource

private object AndroidMainString0 {
  public val android_str: StringResource by 
      lazy { init_android_str() }
}

@InternalResourceApi
internal fun _collectAndroidMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("android_str", AndroidMainString0.android_str)
}

internal val Res.string.android_str: StringResource
  get() = AndroidMainString0.android_str

private fun init_android_str(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:android_str", "android_str",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/app.group.resources_test.generated.resources/values/strings.androidMain.cvr",
    10, 39),
    )
)
