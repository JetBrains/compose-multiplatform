package example.todo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import kotlin.js.Date
import kotlin.math.sqrt

@Composable
fun <T : Any> Crossfade(target: T, attrs: AttrBuilderContext<*> = {}, content: @Composable (T) -> Unit) {
    val holder = remember { TargetHolder<T>(null) }
    val previousTarget: T? = holder.value

    if (previousTarget == null) {
        holder.value = target
        Div(attrs = attrs) {
            content(target)
        }
        return
    }

    if (previousTarget == target) {
        Div(attrs = attrs) {
            content(target)
        }
        return
    }

    holder.value = target

    val animationFactor by animateFloatFactor(key = target, durationMillis = 300L, easing = ::sqrt)

    Div(attrs = attrs) {
        if (animationFactor < 1F) {
            Div(
                attrs = {
                    style {
                        width(100.percent)
                        height(100.percent)
                        position(Position.Absolute)
                        top(0.px)
                        left(0.px)
                        opacity(1F - animationFactor)
                    }
                }
            ) {
                content(previousTarget)
            }
        }

        Div(
            attrs = {
                style {
                    width(100.percent)
                    height(100.percent)
                    position(Position.Absolute)
                    top(0.px)
                    left(0.px)
                    if (animationFactor < 1F) {
                        opacity(animationFactor)
                    }
                }
            }
        ) {
            content(target)
        }
    }
}

private class TargetHolder<T : Any>(
    var value: T?
)

@Composable
private fun animateFloatFactor(key: Any, durationMillis: Long, easing: Easing = Easing { it }): State<Float> {
    val state = remember(key) { mutableStateOf(0F) }

    LaunchedEffect(key) {
        var date = Date.now()
        val startMillis = date
        val endMillis = startMillis + durationMillis.toDouble()
        while (true) {
            date = Date.now()
            if (date >= endMillis) {
                break
            }

            state.value = easing.transform(((date - startMillis) / durationMillis.toDouble()).toFloat())
            delay(16L)
        }

        state.value = 1F
    }

    return state
}

private fun interface Easing {
    fun transform(fraction: Float): Float
}
