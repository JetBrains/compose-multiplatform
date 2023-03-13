/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.addSubview
import platform.UIKit.backgroundColor
import platform.UIKit.insertSubview
import platform.UIKit.removeFromSuperview
import platform.UIKit.setFrame
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}
private val NoOpUpdate: UIView.() -> Unit = STUB_CALLBACK_WITH_RECEIVER
private val NoOpDispose: UIView.() -> Unit = STUB_CALLBACK_WITH_RECEIVER
private val DefaultResize: UIView.(CValue<CGRect>) -> Unit = { rect -> this.setFrame(rect) }

/**
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param background A color of UIView background.
 * @param update A callback to be invoked after the layout is inflated.
 * @param dispose A callback that will be called when the view leaves the composition.
 * @param resize May be used to custom resize logic.
 * @param factory The block creating the [UIView] to be composed.
 * TODO adapt UIKitInteropView to reuse inside LazyColumn like in Android:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/viewinterop/package-summary#AndroidView(kotlin.Function1,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Function1,kotlin.Function1)
 */
@Composable
fun <T : UIView> UIKitInteropView(
    modifier: Modifier,
    background: Color = Color.White,
    update: (T) -> Unit = NoOpUpdate,
    dispose: (T) -> Unit = NoOpDispose,
    resize: (view: T, rect: CValue<CGRect>) -> Unit = DefaultResize,
    factory: () -> T,
) {
    val componentInfo = remember { ComponentInfo<T>() }
    val root = LocalLayerContainer.current
    val density = LocalDensity.current.density
    var rectInPixels by remember { mutableStateOf(IntRect(0, 0, 0, 0)) }
    var localToWindowOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
    Place(
        modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            localToWindowOffset = coordinates.localToWindow(Offset.Zero).round()
            val newRectInPixels = IntRect(localToWindowOffset, coordinates.size)
            if (rectInPixels != newRectInPixels) {
                val rect = newRectInPixels / density
                componentInfo.container.setFrame(rect.toCGRect())
                if (rectInPixels.width != newRectInPixels.width || rectInPixels.height != newRectInPixels.height) {
                    resize(
                        componentInfo.component,
                        CGRectMake(0.0, 0.0, rect.width.toDouble(), rect.height.toDouble()),
                    )
                }
                rectInPixels = newRectInPixels
            }
        }.drawBehind {
            drawRect(Color.Transparent, blendMode = BlendMode.DstAtop) // draw transparent hole
        }
    )

    DisposableEffect(factory, dispose) {
        componentInfo.component = factory()
        componentInfo.container = UIView().apply {
            addSubview(componentInfo.component)
        }
        componentInfo.updater = Updater(componentInfo.component, update)
        root.insertSubview(componentInfo.container, 0)
        onDispose {
            componentInfo.container.removeFromSuperview()
            componentInfo.updater.dispose()
            dispose(componentInfo.component)
        }
    }
    LaunchedEffect(background) {
        componentInfo.container.backgroundColor = parseColor(background)
    }
    SideEffect {
        componentInfo.updater.update = update
    }
}

@Composable
private fun Place(modifier: Modifier) {
    Layout({}, measurePolicy = PlaceMeasurePolicy, modifier = modifier)
}

private object PlaceMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints) =
        layout(constraints.maxWidth, constraints.maxHeight) {}
}

private fun parseColor(color: Color): UIColor {
    return UIColor(
        red = color.red.toDouble(),
        green = color.green.toDouble(),
        blue = color.blue.toDouble(),
        alpha = color.alpha.toDouble()
    )
}

private class ComponentInfo<T : UIView> {
    lateinit var container: UIView
    lateinit var component: T
    lateinit var updater: Updater<T>
}

private class Updater<T : UIView>(
    private val component: T,
    update: (T) -> Unit
) {
    private var isDisposed = false
    private val isUpdateScheduled = atomic(false)
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    private val scheduleUpdate = { _: T ->
        if (!isUpdateScheduled.getAndSet(true)) {
            dispatch_async(dispatch_get_main_queue()) {
                isUpdateScheduled.value = false
                if (!isDisposed) {
                    performUpdate()
                }
            }
        }
    }

    var update: (T) -> Unit = update
        set(value) {
            if (field != value) {
                field = value
                performUpdate()
            }
        }

    private fun performUpdate() {
        // don't replace scheduleUpdate by lambda reference,
        // scheduleUpdate should always be the same instance
        snapshotObserver.observeReads(component, scheduleUpdate) {
            update(component)
        }
    }

    init {
        snapshotObserver.start()
        performUpdate()
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
        isDisposed = true
    }
}
