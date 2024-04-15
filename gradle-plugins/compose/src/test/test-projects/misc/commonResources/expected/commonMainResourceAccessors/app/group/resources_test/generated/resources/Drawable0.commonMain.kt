@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import org.jetbrains.compose.resources.DrawableResource

private object CommonMainDrawable0 {
  public val _3_strange_name: DrawableResource by
  lazy { init__3_strange_name() }

  public val camelCaseName: DrawableResource by
  lazy { init_camelCaseName() }

  public val vector: DrawableResource by
  lazy { init_vector() }

  public val vector_2: DrawableResource by
  lazy { init_vector_2() }
}

internal val Res.drawable._3_strange_name: DrawableResource
  get() = CommonMainDrawable0._3_strange_name

private fun init__3_strange_name(): DrawableResource =
  org.jetbrains.compose.resources.DrawableResource(
    "drawable:_3_strange_name",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/3-strange-name.xml", -1, -1),
    )
  )

internal val Res.drawable.camelCaseName: DrawableResource
  get() = CommonMainDrawable0.camelCaseName

private fun init_camelCaseName(): DrawableResource =
  org.jetbrains.compose.resources.DrawableResource(
    "drawable:camelCaseName",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/camelCaseName.xml", -1, -1),
    )
  )

internal val Res.drawable.vector: DrawableResource
  get() = CommonMainDrawable0.vector

private fun init_vector(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:vector",
  setOf(

    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("ast"),
    ), "drawable-ast/vector.xml", -1, -1),

    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("au"),
      org.jetbrains.compose.resources.RegionQualifier("US"), ), "drawable-au-rUS/vector.xml", -1, -1),

    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.ThemeQualifier.DARK,
      org.jetbrains.compose.resources.LanguageQualifier("ge"), ),
      "drawable-dark-ge/vector.xml", -1, -1),

    org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("en"),
    ), "drawable-en/vector.xml", -1, -1),
    org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/vector.xml", -1, -1),
  )
)

internal val Res.drawable.vector_2: DrawableResource
  get() = CommonMainDrawable0.vector_2

private fun init_vector_2(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:vector_2",
  setOf(
    org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/vector_2.xml", -1, -1),
  )
)