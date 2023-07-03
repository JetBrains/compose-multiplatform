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

package androidx.compose.ui.awt

import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import javax.accessibility.Accessible
import javax.swing.SwingUtilities
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * Provides a heavyweight AWT [component] used to render content (from [setContent]) on-screen with Skia.
 *
 * If smooth interop with Swing is needed, consider using [androidx.compose.ui.awt.SwingComposeBridge]
 */
internal class WindowComposeBridge(
    private val skiaLayerAnalytics: SkiaLayerAnalytics
) : ComposeBridge() {
    /**
     * See also backend layer for swing interop in [androidx.compose.ui.awt.SwingComposeBridge]
     */
    override val component: SkiaLayer = object : SkiaLayer(
        externalAccessibleFactory = { sceneAccessible },
        analytics = skiaLayerAnalytics
    ), Accessible {
        override fun addNotify() {
            super.addNotify()
            resetSceneDensity()
            initContent()
            updateSceneSize()
            setParentWindow(SwingUtilities.getWindowAncestor(this))
        }

        override fun removeNotify() {
            setParentWindow(null)
            super.removeNotify()
        }

        override fun paint(g: Graphics) {
            resetSceneDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = currentInputMethodRequests

        override fun doLayout() {
            super.doLayout()
            updateSceneSize()
        }

        override fun getPreferredSize(): Dimension {
            return if (isPreferredSizeSet) super.getPreferredSize() else sceneDimension
        }
    }

    override val renderApi: GraphicsApi
        get() = component.renderApi

    override val clipComponents: MutableList<ClipRectangle>
        get() = component.clipComponents

    override val focusComponentDelegate: Component
        get() = component.canvas

    init {
        component.skikoView = skikoView
        attachComposeToComponent()
    }

    override fun requestNativeFocusOnAccessible(accessible: Accessible) {
        component.requestNativeFocusOnAccessible(accessible)
    }

    override fun onComposeInvalidation() {
        component.needRedraw()
    }

    override fun disposeComponentLayer() {
        component.dispose()
    }
}
