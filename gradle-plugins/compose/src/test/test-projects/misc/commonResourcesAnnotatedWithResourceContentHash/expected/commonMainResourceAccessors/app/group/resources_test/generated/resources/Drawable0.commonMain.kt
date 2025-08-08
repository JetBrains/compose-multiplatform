@file:OptIn(InternalResourceApi::class)

package app.group.resources_test.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/app.group.resources_test.generated.resources/"

@delegate:ResourceContentHash(-1_557_874_605)
internal val Res.drawable.raster: DrawableResource by lazy {
      DrawableResource("drawable:raster", setOf(
        ResourceItem(setOf(), "${MD}drawable/raster.png", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("raster", Res.drawable.raster)
}
