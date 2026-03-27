@file:OptIn(InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/my.lib.res/"

public val MyRes.plurals.numberOfSongsAvailable: PluralStringResource by lazy {
      PluralStringResource("plurals:numberOfSongsAvailable", "numberOfSongsAvailable", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 10, 124),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainPlurals0Resources(map: MutableMap<String, PluralStringResource>) {
  map.put("numberOfSongsAvailable", MyRes.plurals.numberOfSongsAvailable)
}
