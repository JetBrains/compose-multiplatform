@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.PluralStringResource

private object CommonMainPlurals0 {
  public val numberOfSongsAvailable: PluralStringResource by 
      lazy { init_numberOfSongsAvailable() }
}

@InternalResourceApi
internal fun _collectCommonMainPlurals0Resources(map: MutableMap<String, PluralStringResource>) {
  map.put("numberOfSongsAvailable", CommonMainPlurals0.numberOfSongsAvailable)
}

internal val Res.plurals.numberOfSongsAvailable: PluralStringResource
  get() = CommonMainPlurals0.numberOfSongsAvailable

private fun init_numberOfSongsAvailable(): PluralStringResource =
    org.jetbrains.compose.resources.PluralStringResource(
  "plurals:numberOfSongsAvailable", "numberOfSongsAvailable",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/app.group.resources_test.generated.resources/values/strings.commonMain.cvr",
    10, 124),
    )
)
