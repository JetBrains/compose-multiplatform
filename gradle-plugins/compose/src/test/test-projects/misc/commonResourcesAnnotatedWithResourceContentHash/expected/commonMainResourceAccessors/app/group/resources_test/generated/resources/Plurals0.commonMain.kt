@file:OptIn(InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/app.group.resources_test.generated.resources/"

@delegate:ResourceContentHash(-759_003_763)
internal val Res.plurals.plurals: PluralStringResource by lazy {
      PluralStringResource("plurals:plurals", "plurals", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 10, 34),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainPlurals0Resources(map: MutableMap<String, PluralStringResource>) {
  map.put("plurals", Res.plurals.plurals)
}
