@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package me.app.jvmonlyresources.generated.resources

import kotlin.OptIn
import org.jetbrains.compose.resources.DrawableResource

private object MainDrawable0 {
    public val vector: DrawableResource by
    lazy { init_vector() }
}

internal val Res.drawable.vector: DrawableResource
    get() = MainDrawable0.vector

private fun init_vector(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
    "drawable:vector",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/vector.xml", -1, -1),
    )
)