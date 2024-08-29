@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package me.app.jvmonlyresources.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi

private object MainDrawable0 {
  public val vector: DrawableResource by 
      lazy { init_vector() }
}

@InternalResourceApi
internal fun _collectMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("vector", MainDrawable0.vector)
}

internal val Res.drawable.vector: DrawableResource
  get() = MainDrawable0.vector

private fun init_vector(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:vector",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
        "composeResources/me.app.jvmonlyresources.generated.resources/drawable/vector.xml", -1, -1),
    )
)
