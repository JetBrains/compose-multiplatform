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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.awt

import androidx.compose.ui.ExperimentalComposeUiApi
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.swing.SkiaSwingLayer

/**
 * Provides [component] that can be used as a Swing component.
 * Content set in [setContent] will be drawn on this [component].
 *
 * [SwingComposeBridge] provides smooth integration with Swing, so z-ordering, double-buffering etc. from Swing will be taken into account.
 *
 * However, if smooth interop with Swing is not needed, consider using [androidx.compose.ui.awt.WindowComposeBridge]
 */
@OptIn(ExperimentalSkikoApi::class)
internal class SwingComposeBridge(
    private val skiaLayerAnalytics: SkiaLayerAnalytics
) : ComposeBridge() {
    /**
     * See also backendLayer for standalone Compose in [androidx.compose.ui.awt.WindowComposeBridge]
     */
    override val component: SkiaSwingLayer =
        object : SkiaSwingLayer(skikoView = skikoView, analytics = skiaLayerAnalytics) {
            override fun addNotify() {
                super.addNotify()
                resetSceneDensity()
                initContent()
                updateSceneSize()
                setParentWindow(SwingUtilities.getWindowAncestor(this))
            }

            override fun removeNotify() {
                setParentWindow(window = null)
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

            override fun getAccessibleContext(): AccessibleContext? {
                return sceneAccessible.accessibleContext
            }
        }

    override val renderApi: GraphicsApi
        get() = component.renderApi

    override val clipComponents: MutableList<ClipRectangle>
        get() = component.clipComponents

    override val focusComponentDelegate: Component
        get() = component

    override fun requestNativeFocusOnAccessible(accessible: Accessible) {
        // TODO: support a11y
    }

    override fun onComposeInvalidation() {
        component.repaint()
    }

    init {
        attachComposeToComponent()
    }

    override fun disposeComponentLayer() {
        component.dispose()
    }
}