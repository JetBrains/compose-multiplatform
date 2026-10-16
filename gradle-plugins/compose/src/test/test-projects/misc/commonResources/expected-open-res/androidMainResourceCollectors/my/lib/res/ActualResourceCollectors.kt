@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import kotlin.String
import kotlin.collections.Map
import kotlin.collections.Set
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource

internal actual val MyRes.supportedLocales: Set<String> by lazy {
  val locales = mutableSetOf<String>()
  _collectAndroidMainResourceLocales(locales)
  _collectCommonMainResourceLocales(locales)
  locales.toSet()
}

public actual val MyRes.allDrawableResources: Map<String, DrawableResource> by lazy {
  val map = mutableMapOf<String, DrawableResource>()
  _collectCommonMainDrawable0Resources(map)
  return@lazy map
}

public actual val MyRes.allStringResources: Map<String, StringResource> by lazy {
  val map = mutableMapOf<String, StringResource>()
  _collectAndroidMainString0Resources(map)
  _collectCommonMainString0Resources(map)
  return@lazy map
}

public actual val MyRes.allStringArrayResources: Map<String, StringArrayResource> by lazy {
  val map = mutableMapOf<String, StringArrayResource>()
  return@lazy map
}

public actual val MyRes.allPluralStringResources: Map<String, PluralStringResource> by lazy {
  val map = mutableMapOf<String, PluralStringResource>()
  _collectCommonMainPlurals0Resources(map)
  return@lazy map
}

public actual val MyRes.allFontResources: Map<String, FontResource> by lazy {
  val map = mutableMapOf<String, FontResource>()
  _collectCommonMainFont0Resources(map)
  return@lazy map
}
