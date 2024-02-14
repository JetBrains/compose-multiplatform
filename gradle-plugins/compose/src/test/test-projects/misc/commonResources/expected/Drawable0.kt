@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

@ExperimentalResourceApi
private object Drawable0 {
  public val _3_strange_name: DrawableResource = org.jetbrains.compose.resources.DrawableResource(
        "drawable:_3_strange_name",
          setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/3-strange-name.xml"),
          )
      )

  public val camelCaseName: DrawableResource = org.jetbrains.compose.resources.DrawableResource(
        "drawable:camelCaseName",
          setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/camelCaseName.xml"),
          )
      )

  public val vector: DrawableResource = org.jetbrains.compose.resources.DrawableResource(
        "drawable:vector",
          setOf(
           
          org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("au"),
          org.jetbrains.compose.resources.RegionQualifier("US"), ), "drawable-au-rUS/vector.xml"),
           
          org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.ThemeQualifier.DARK,
          org.jetbrains.compose.resources.LanguageQualifier("ge"), ),
          "drawable-dark-ge/vector.xml"),
           
          org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("en"),
          ), "drawable-en/vector.xml"),
            org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/vector.xml"),
          )
      )

  public val vector_2: DrawableResource = org.jetbrains.compose.resources.DrawableResource(
        "drawable:vector_2",
          setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/vector_2.xml"),
          )
      )
}

@ExperimentalResourceApi
internal val Res.drawable._3_strange_name: DrawableResource
  get() = Drawable0._3_strange_name

@ExperimentalResourceApi
internal val Res.drawable.camelCaseName: DrawableResource
  get() = Drawable0.camelCaseName

@ExperimentalResourceApi
internal val Res.drawable.vector: DrawableResource
  get() = Drawable0.vector

@ExperimentalResourceApi
internal val Res.drawable.vector_2: DrawableResource
  get() = Drawable0.vector_2
