@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi

private object CommonMainFont0 {
  public val emptyFont: FontResource by 
      lazy { init_emptyFont() }
}

@InternalResourceApi
internal fun _collectCommonMainFont0Resources(map: MutableMap<String, FontResource>) {
  map.put("emptyFont", CommonMainFont0.emptyFont)
}

internal val Res.font.emptyFont: FontResource
  get() = CommonMainFont0.emptyFont

private fun init_emptyFont(): FontResource = org.jetbrains.compose.resources.FontResource(
  "font:emptyFont",
    setOf(
     
    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("en"),
    ),
    "composeResources/app.group.resources_test.generated.resources/font-en/emptyFont.otf", -1, -1),
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/app.group.resources_test.generated.resources/font/emptyFont.otf", -1, -1),
    )
)
