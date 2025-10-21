@file:OptIn(InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.ThemeQualifier

private const val MD: String = "composeResources/my.lib.res/"

public val MyRes.drawable._3_strange_name: DrawableResource by lazy {
      DrawableResource("drawable:_3_strange_name", setOf(
        ResourceItem(setOf(), "${MD}drawable/3-strange-name.xml", -1, -1),
      ))
    }

public val MyRes.drawable.camelCaseName: DrawableResource by lazy {
      DrawableResource("drawable:camelCaseName", setOf(
        ResourceItem(setOf(), "${MD}drawable/camelCaseName.xml", -1, -1),
      ))
    }

public val MyRes.drawable.`is`: DrawableResource by lazy {
      DrawableResource("drawable:is", setOf(
        ResourceItem(setOf(), "${MD}drawable/is.xml", -1, -1),
      ))
    }

public val MyRes.drawable.raster: DrawableResource by lazy {
      DrawableResource("drawable:raster", setOf(
        ResourceItem(setOf(), "${MD}drawable/raster.webp", -1, -1),
      ))
    }

public val MyRes.drawable.svg: DrawableResource by lazy {
      DrawableResource("drawable:svg", setOf(
        ResourceItem(setOf(), "${MD}drawable/svg.svg", -1, -1),
      ))
    }

public val MyRes.drawable.vector: DrawableResource by lazy {
      DrawableResource("drawable:vector", setOf(
        ResourceItem(setOf(LanguageQualifier("ast"), ), "${MD}drawable-ast/vector.xml", -1, -1),
        ResourceItem(setOf(LanguageQualifier("au"), RegionQualifier("US"), ), "${MD}drawable-au-rUS/vector.xml", -1, -1),
        ResourceItem(setOf(ThemeQualifier.DARK, LanguageQualifier("ge"), ), "${MD}drawable-dark-ge/vector.xml", -1, -1),
        ResourceItem(setOf(LanguageQualifier("en"), ), "${MD}drawable-en/vector.xml", -1, -1),
        ResourceItem(setOf(), "${MD}drawable/vector.xml", -1, -1),
      ))
    }

public val MyRes.drawable.vector_2: DrawableResource by lazy {
      DrawableResource("drawable:vector_2", setOf(
        ResourceItem(setOf(), "${MD}drawable/vector_2.xml", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("_3_strange_name", MyRes.drawable._3_strange_name)
  map.put("camelCaseName", MyRes.drawable.camelCaseName)
  map.put("is", MyRes.drawable.`is`)
  map.put("raster", MyRes.drawable.raster)
  map.put("svg", MyRes.drawable.svg)
  map.put("vector", MyRes.drawable.vector)
  map.put("vector_2", MyRes.drawable.vector_2)
}
