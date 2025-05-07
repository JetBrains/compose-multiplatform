@file:OptIn(InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/my.lib.res/"

public val MyRes.font.emptyFont: FontResource by lazy {
      FontResource("font:emptyFont", setOf(
        ResourceItem(setOf(LanguageQualifier("en"), ), "${MD}font-en/emptyFont.otf", -1, -1),
        ResourceItem(setOf(), "${MD}font/emptyFont.otf", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainFont0Resources(map: MutableMap<String, FontResource>) {
  map.put("emptyFont", MyRes.font.emptyFont)
}
