package org.jetbrains.compose.movable

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

interface SingleDirectionMoveScope {
    fun moveBy(pixels: Float): Float
}

interface SingleDirectionMovable {

    suspend fun move(
        movePriority: MutatePriority = MutatePriority.Default,
        block: suspend SingleDirectionMoveScope.() -> Unit
    )

    val isMoveInProgress: Boolean

}

interface SingleDirectionRawMovementDispatcher : SingleDirectionMovable {
    fun dispatchRawMovement(delta: Float): Float
}

typealias SingleDirectionMoveDeltaConsumer = (Float) -> Float

fun movableState(
    consumeMoveDelta: SingleDirectionMoveDeltaConsumer
): SingleDirectionMovable {
    return DefaultSingleDirectionMovableState(consumeMoveDelta)
}

@Composable
fun rememberMovableState(
    consumeMoveDelta: SingleDirectionMoveDeltaConsumer
): SingleDirectionMovable {
    return DefaultSingleDirectionMovableState(consumeMoveDelta)
}

suspend fun SingleDirectionMovable.stopMovement(
    movePriority: MutatePriority = MutatePriority.Default
) {
    move(movePriority){}
}

private class DefaultSingleDirectionMovableState(
    val onMoveDelta: SingleDirectionMoveDeltaConsumer
) : SingleDirectionRawMovementDispatcher {

    private val singleDirectionMoveScope = object : SingleDirectionMoveScope {
        override fun moveBy(pixels: Float): Float = onMoveDelta(pixels)
    }

    private val moveMutex = MutatorMutex()

    private val isMovingState = mutableStateOf(false)

    override suspend fun move(
        movePriority: MutatePriority,
        block: suspend SingleDirectionMoveScope.() -> Unit
    ) {
        moveMutex.mutateWith(singleDirectionMoveScope, movePriority) {
            isMovingState.value = true
            block()
            isMovingState.value = false
        }
    }

    override val isMoveInProgress: Boolean
        get() = isMovingState.value

    override fun dispatchRawMovement(delta: Float): Float {
        return onMoveDelta(delta)
    }
}