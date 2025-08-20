@file:OptIn(InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource

private const val MD: String = "composeResources/app.group.resources_test.generated.resources/"

@delegate:ResourceContentHash(50_967_853)
internal val Res.string.android_str: StringResource by lazy {
      StringResource("string:android_str", "android_str", setOf(
        ResourceItem(setOf(), "${MD}values/strings.androidMain.cvr", 10, 39),
      ))
    }

@InternalResourceApi
internal fun _collectAndroidMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("android_str", Res.string.android_str)
}
