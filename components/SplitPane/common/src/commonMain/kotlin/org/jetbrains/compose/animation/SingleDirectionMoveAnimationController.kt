package org.jetbrains.compose.animation

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.AtomicReference
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SingleDirectionMoveAnimationController(
    internal val consumeMoveDelta: (Float) -> Float,
    animationClockObservable: AnimationClockObservable,
    internal val interactionState: InteractionState? = null
) : SingleDirectionMovable {

    fun smoothMoveBy(
        value: Float,
        spec: AnimationSpec<Float> = SpringSpec(),
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        val to = animatedFloat.value + value
        animatedFloat.animateTo(to, spec, onEnd)
    }

    private val moveControlJob = AtomicReference<Job?>(null)

    private val moveControlMutex = Mutex()

    private val moveScope: SingleDirectionMoveScope = object : SingleDirectionMoveScope {
        override fun moveBy(pixels: Float): Float = consumeMoveDelta(pixels)
    }

    override suspend fun move(
        block: suspend SingleDirectionMoveScope.() -> Unit
    ): Unit = coroutineScope {
        animatedFloat.stop()
        val currentJob = coroutineContext[Job]
        moveControlJob.getAndSet(currentJob)?.cancel()
        moveControlMutex.withLock {
            isAnimationRunningState.value = true
            moveScope.block()
            isAnimationRunningState.value = false
        }
    }

    private val isAnimationRunningState = mutableStateOf(false)

    private val clockProxy = object : AnimationClockObservable {
        override fun subscribe(observer: AnimationClockObserver) {
            isAnimationRunningState.value = true
            animationClockObservable.subscribe(observer)
        }

        override fun unsubscribe(observer: AnimationClockObserver) {
            isAnimationRunningState.value = false
            animationClockObservable.unsubscribe(observer)
        }
    }

    val isAnimationRunning
        get() = isAnimationRunningState.value

    fun stopAnimation() {
        animatedFloat.stop()
        moveControlJob.getAndSet(null)?.cancel()
    }

    private val animatedFloat = DeltaAnimatedFloat(0f,clockProxy,consumeMoveDelta)

    internal var value: Float
        get() = animatedFloat.value
        set(value) {
            animatedFloat.snapTo(value)
        }

}