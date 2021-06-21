/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.util.AddRemoveMutableList
import java.awt.Menu
import java.awt.MenuItem
import javax.swing.JComponent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

/**
 * Composes the given composable into the MenuBar.
 *
 * The new composition can be logically "linked" to an existing one, by providing a
 * [parentComposition]. This will ensure that invalidations and CompositionLocals will flow
 * through the two compositions as if they were not separate.
 *
 * @param parentComposition The parent composition reference to coordinate
 *        scheduling of composition updates.
 *        If null then default root composition will be used.
 * @param content Composable content of the MenuBar.
 */
fun JMenuBar.setContent(
    parentComposition: CompositionContext,
    content: @Composable (MenuBarScope.() -> Unit)
): Composition {
    val applier = MutableListApplier(asMutableList())
    val composition = Composition(applier, parentComposition)
    val scope = MenuBarScope()
    composition.setContent {
        scope.content()
    }
    return composition
}

/**
 * Composes the given composable into the Menu.
 *
 * The new composition can be logically "linked" to an existing one, by providing a
 * [parentComposition]. This will ensure that invalidations and CompositionLocals will flow
 * through the two compositions as if they were not separate.
 *
 * @param parentComposition The parent composition reference to coordinate
 *        scheduling of composition updates.
 *        If null then default root composition will be used.
 * @param content Composable content of the Menu.
 */
fun Menu.setContent(
    parentComposition: CompositionContext,
    content: @Composable (MenuScope.() -> Unit)
): Composition {
    val applier = MutableListApplier(asMutableList())
    val composition = Composition(applier, parentComposition)
    val scope = MenuScope(AwtMenuScope())
    composition.setContent {
        scope.content()
    }
    return composition
}

/**
 * Composes the given composable into the Menu.
 *
 * The new composition can be logically "linked" to an existing one, by providing a
 * [parentComposition]. This will ensure that invalidations and CompositionLocals will flow
 * through the two compositions as if they were not separate.
 *
 * @param parentComposition The parent composition reference to coordinate
 *        scheduling of composition updates.
 *        If null then default root composition will be used.
 * @param content Composable content of the Menu.
 */
fun JMenu.setContent(
    parentComposition: CompositionContext,
    content: @Composable (MenuScope.() -> Unit)
): Composition {
    val applier = MutableListApplier(asMutableList())
    val composition = Composition(applier, parentComposition)
    val scope = MenuScope(SwingMenuScope())
    composition.setContent {
        scope.content()
    }
    return composition
}

// This menu is used by Tray
@Composable
private fun AwtMenu(
    text: String,
    enabled: Boolean,
    content: @Composable MenuScope.() -> Unit
) {
    val menu = remember(::Menu)
    val compositionContext = rememberCompositionContext()

    DisposableEffect(Unit) {
        val composition = menu.setContent(compositionContext, content)
        onDispose {
            composition.dispose()
        }
    }

    ComposeNode<Menu, MutableListApplier<MenuItem>>(
        factory = { menu },
        update = {
            set(text, Menu::setLabel)
            set(enabled, Menu::setEnabled)
        }
    )
}

@Composable
private fun SwingMenu(
    text: String,
    enabled: Boolean,
    content: @Composable MenuScope.() -> Unit
) {
    val menu = remember(::JMenu)
    val compositionContext = rememberCompositionContext()

    DisposableEffect(Unit) {
        val composition = menu.setContent(compositionContext, content)
        onDispose {
            composition.dispose()
        }
    }

    ComposeNode<JMenu, MutableListApplier<JComponent>>(
        factory = { menu },
        update = {
            set(text, JMenu::setText)
            set(enabled, JMenu::setEnabled)
        }
    )
}

// TODO(demin): consider making MenuBarScope/MenuScope as an interface
//  after b/165812010 will be fixed
/**
 * Receiver scope which is used by [JMenuBar.setContent] and [WindowScope.MenuBar].
 */
class MenuBarScope internal constructor() {
    /**
     * Adds menu to the menu bar
     *
     * @param text text of the menu that will be shown on the menu bar
     * @param enabled is this menu item can be chosen
     * @param content content of the menu (sub menus, items, separators, etc)
     */
    @Composable
    fun Menu(
        text: String,
        enabled: Boolean = true,
        content: @Composable MenuScope.() -> Unit
    ): Unit = SwingMenu(
        text,
        enabled,
        content
    )
}

interface MenuScopeImpl {
    @Composable
    fun Menu(
        text: String,
        enabled: Boolean,
        content: @Composable MenuScope.() -> Unit
    )

    @Composable
    fun Separator()

    @Composable
    fun Item(
        text: String,
        enabled: Boolean,
        onClick: () -> Unit
    )
}

private class AwtMenuScope : MenuScopeImpl {
    /**
     * Adds sub menu to the menu
     *
     * @param text text of the menu that will be shown in the menu
     * @param enabled is this menu item can be chosen
     * @param content content of the menu (sub menus, items, separators, etc)
     */
    @Composable
    override fun Menu(
        text: String,
        enabled: Boolean,
        content: @Composable MenuScope.() -> Unit
    ): Unit = AwtMenu(
        text,
        enabled,
        content
    )

    @Composable
    override fun Separator() {
        ComposeNode<MenuItem, MutableListApplier<MenuItem>>(
            // item with name "-" has different look
            factory = { MenuItem("-") },
            update = {}
        )
    }

    @Composable
    override fun Item(
        text: String,
        enabled: Boolean,
        onClick: () -> Unit
    ) {
        val currentOnClick by rememberUpdatedState(onClick)

        ComposeNode<MenuItem, MutableListApplier<MenuItem>>(
            factory = {
                MenuItem().apply {
                    addActionListener {
                        currentOnClick()
                    }
                }
            },
            update = {
                set(text, MenuItem::setLabel)
                set(enabled, MenuItem::setEnabled)
            }
        )
    }
}

private class SwingMenuScope : MenuScopeImpl {
    /**
     * Adds sub menu to the menu
     *
     * @param text text of the menu that will be shown in the menu
     * @param enabled is this menu item can be chosen
     * @param content content of the menu (sub menus, items, separators, etc)
     */
    @Composable
    override fun Menu(
        text: String,
        enabled: Boolean,
        content: @Composable MenuScope.() -> Unit
    ): Unit = SwingMenu(
        text,
        enabled,
        content
    )

    @Composable
    override fun Separator() {
        ComposeNode<JComponent, MutableListApplier<JComponent>>(
            // item with name "-" has different look
            factory = { JPopupMenu.Separator() },
            update = {}
        )
    }

    @Composable
    override fun Item(
        text: String,
        enabled: Boolean,
        onClick: () -> Unit
    ) {
        val currentOnClick by rememberUpdatedState(onClick)

        ComposeNode<JMenuItem, MutableListApplier<JComponent>>(
            factory = {
                JMenuItem().apply {
                    addActionListener {
                        currentOnClick()
                    }
                }
            },
            update = {
                set(text, JMenuItem::setText)
                set(enabled, JMenuItem::setEnabled)
            }
        )
    }
}

// we use `class MenuScope` and `interface MenuScopeImpl` instead of just `interface MenuScope`
// because of b/165812010
/**
 * Receiver scope which is used by [Menu.setContent], [MenuBarScope.Menu], [Tray]
 */
class MenuScope internal constructor(private val impl: MenuScopeImpl) {
    /**
     * Adds sub menu to the menu
     *
     * @param text text of the menu that will be shown in the menu
     * @param enabled is this menu item can be chosen
     * @param content content of the menu (sub menus, items, separators, etc)
     */
    @Composable
    fun Menu(
        text: String,
        enabled: Boolean = true,
        content: @Composable MenuScope.() -> Unit
    ): Unit = impl.Menu(
        text,
        enabled,
        content
    )

    /**
     * Adds separator to the menu
     */
    @Composable
    fun Separator() = impl.Separator()

    // TODO(demin): implement shortcuts
    /**
     * Adds item to the menu
     *
     * @param text text of the item that will be shown in the menu
     * @param enabled is this item item can be chosen
     * @param onClick action that should be performed when the user clicks on the item
     */
    @Composable
    fun Item(
        text: String,
        enabled: Boolean = true,
        onClick: () -> Unit
    ): Unit = impl.Item(text, enabled, onClick)
}

private class MutableListApplier<T>(
    private val list: MutableList<T>
) : AbstractApplier<T?>(null) {
    override fun insertTopDown(index: Int, instance: T?) {
        list.add(index, instance!!)
    }

    override fun insertBottomUp(index: Int, instance: T?) {
        // Ignore, we have plain list
    }

    override fun remove(index: Int, count: Int) {
        for (i in index + count - 1 downTo index) {
            list.removeAt(i)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun move(from: Int, to: Int, count: Int) {
        (list as MutableList<T?>).move(from, to, count)
    }

    override fun onClear() {
        list.clear()
    }
}

private fun JMenuBar.asMutableList(): MutableList<JComponent> {
    return object : AddRemoveMutableList<JComponent>() {
        override val size: Int get() = this@asMutableList.menuCount
        override fun get(index: Int) = this@asMutableList.getMenu(index)

        override fun performAdd(element: JComponent) {
            this@asMutableList.add(element)
        }

        override fun performRemove(index: Int) {
            this@asMutableList.remove(index)
        }
    }
}

private fun JMenu.asMutableList(): MutableList<JComponent> {
    return object : AddRemoveMutableList<JComponent>() {
        override val size: Int get() = this@asMutableList.itemCount
        override fun get(index: Int) = this@asMutableList.getMenuComponent(index) as JComponent

        override fun performAdd(element: JComponent) {
            this@asMutableList.add(element)
        }

        override fun performRemove(index: Int) {
            this@asMutableList.remove(index)
        }
    }
}

private fun Menu.asMutableList(): MutableList<MenuItem> {
    return object : AddRemoveMutableList<MenuItem>() {
        override val size: Int get() = this@asMutableList.itemCount
        override fun get(index: Int) = this@asMutableList.getItem(index)

        override fun performAdd(element: MenuItem) {
            this@asMutableList.add(element)
        }

        override fun performRemove(index: Int) {
            this@asMutableList.remove(index)
        }
    }
}