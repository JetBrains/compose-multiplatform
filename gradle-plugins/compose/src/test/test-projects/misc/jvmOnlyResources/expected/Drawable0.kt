@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package me.app.jvmonlyresources.generated.resources

import kotlin.OptIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

@ExperimentalResourceApi
private object Drawable0 {
    public val vector: DrawableResource = org.jetbrains.compose.resources.DrawableResource(
        "drawable:vector",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/vector.xml"),
        )
    )
}

@ExperimentalResourceApi
internal val Res.drawable.vector: DrawableResource
    get() = Drawable0.vector