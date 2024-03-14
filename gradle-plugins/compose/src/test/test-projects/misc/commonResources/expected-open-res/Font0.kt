@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.FontResource

@ExperimentalResourceApi
private object Font0 {
  public val emptyFont: FontResource by 
      lazy { init_emptyFont() }
}

@ExperimentalResourceApi
public val Res.font.emptyFont: FontResource
  get() = Font0.emptyFont

@ExperimentalResourceApi
private fun init_emptyFont(): FontResource = org.jetbrains.compose.resources.FontResource(
  "font:emptyFont",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(), "font/emptyFont.otf"),
    )
)
