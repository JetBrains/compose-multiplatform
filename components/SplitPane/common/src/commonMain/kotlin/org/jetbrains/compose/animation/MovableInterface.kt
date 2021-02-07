package org.jetbrains.compose.animation

interface SingleDirectionMoveScope {
    fun moveBy(pixels: Float): Float
}

interface SingleDirectionMovable {
    suspend fun move(
        block: suspend SingleDirectionMoveScope.() -> Unit
    ): Unit
}