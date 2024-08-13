@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi

private object CommonMainDrawable0 {
  public val _3_strange_name: DrawableResource by 
      lazy { init__3_strange_name() }

  public val camelCaseName: DrawableResource by 
      lazy { init_camelCaseName() }

  public val `is`: DrawableResource by 
      lazy { init_is() }

  public val vector: DrawableResource by 
      lazy { init_vector() }

  public val vector_2: DrawableResource by 
      lazy { init_vector_2() }
}

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("_3_strange_name", CommonMainDrawable0._3_strange_name)
  map.put("camelCaseName", CommonMainDrawable0.camelCaseName)
  map.put("is", CommonMainDrawable0.`is`)
  map.put("vector", CommonMainDrawable0.vector)
  map.put("vector_2", CommonMainDrawable0.vector_2)
}

public val Res.drawable._3_strange_name: DrawableResource
  get() = CommonMainDrawable0._3_strange_name

private fun init__3_strange_name(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:_3_strange_name",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/drawable/3-strange-name.xml", -1, -1),
    )
)

public val Res.drawable.camelCaseName: DrawableResource
  get() = CommonMainDrawable0.camelCaseName

private fun init_camelCaseName(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:camelCaseName",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/drawable/camelCaseName.xml", -1, -1),
    )
)

public val Res.drawable.`is`: DrawableResource
  get() = CommonMainDrawable0.`is`

private fun init_is(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:is",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/drawable/is.xml", -1, -1),
    )
)

public val Res.drawable.vector: DrawableResource
  get() = CommonMainDrawable0.vector

private fun init_vector(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:vector",
    setOf(
     
    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("ast"),
    ), "composeResources/my.lib.res/drawable-ast/vector.xml", -1, -1),
     
    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("au"),
    org.jetbrains.compose.resources.RegionQualifier("US"), ),
    "composeResources/my.lib.res/drawable-au-rUS/vector.xml", -1, -1),
     
    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.ThemeQualifier.DARK,
    org.jetbrains.compose.resources.LanguageQualifier("ge"), ),
    "composeResources/my.lib.res/drawable-dark-ge/vector.xml", -1, -1),
     
    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("en"),
    ), "composeResources/my.lib.res/drawable-en/vector.xml", -1, -1),
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/drawable/vector.xml", -1, -1),
    )
)

public val Res.drawable.vector_2: DrawableResource
  get() = CommonMainDrawable0.vector_2

private fun init_vector_2(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:vector_2",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/my.lib.res/drawable/vector_2.xml", -1, -1),
    )
)
