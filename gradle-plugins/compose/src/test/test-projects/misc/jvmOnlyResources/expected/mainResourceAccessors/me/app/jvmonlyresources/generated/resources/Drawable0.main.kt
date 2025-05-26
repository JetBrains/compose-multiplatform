@file:OptIn(InternalResourceApi::class)

package me.app.jvmonlyresources.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/me.app.jvmonlyresources.generated.resources/"

internal val Res.drawable.vector: DrawableResource by lazy {
      DrawableResource("drawable:vector", setOf(
        ResourceItem(setOf(), "${MD}drawable/vector.xml", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("vector", Res.drawable.vector)
}
