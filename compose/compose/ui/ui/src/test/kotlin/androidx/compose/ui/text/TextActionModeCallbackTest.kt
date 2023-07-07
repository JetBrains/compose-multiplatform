/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.ActionProvider
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.actionmodecallback.MenuItemOption
import androidx.compose.ui.platform.actionmodecallback.TextActionModeCallback
import androidx.test.filters.SdkSuppress
import org.junit.Test
import com.google.common.truth.Truth.assertThat

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
class TextActionModeCallbackTest {
    @Test
    fun addMenuItem_correctValues() {
        val callback = TextActionModeCallback(
            onCopyRequested = { /* copy */ }
        )
        val menu = ItemTrackingFakeMenu()
        callback.addMenuItem(menu, MenuItemOption.Copy)

        val resultItem = menu.menuItems.first()

        assertThat(resultItem.itemId).isEqualTo(MenuItemOption.Copy.id)
        assertThat(resultItem.order).isEqualTo(MenuItemOption.Copy.id)
        assertThat(resultItem.title).isEqualTo(MenuItemOption.Copy.titleResource.toString())
    }

    @Test
    fun updateMenuItems_hasItem_shouldDeleteItem() {
        val menu = ItemTrackingFakeMenu()
        menu.menuItems.add(FakeMenuItem(MenuItemOption.Copy))

        val callback = TextActionModeCallback({})
        callback.updateMenuItems(menu)

        assertThat(menu.menuItems.isEmpty())
    }

    @Test
    fun updateMenuItems_shouldAddItem() {
        val menu = ItemTrackingFakeMenu()

        val callback = TextActionModeCallback(onCopyRequested = { /* copy */ })
        callback.updateMenuItems(menu)

        assertThat(menu.menuItems.filter { it.itemId == MenuItemOption.Copy.id }.size).isEqualTo(1)
    }

    @Test
    fun updateMenuItems_hasItem_shouldAddItem_doesNothing() {
        val menu = ItemTrackingFakeMenu()
        menu.menuItems.add(FakeMenuItem(MenuItemOption.Copy))
        assertThat(menu.menuItems.size).isEqualTo(1)
        val previousItem = menu.menuItems.first()

        val callback = TextActionModeCallback(onCopyRequested = { /* copy */ })
        callback.updateMenuItems(menu)

        assertThat(menu.menuItems.size).isEqualTo(1)
        val currentItem = menu.menuItems.first()
        assertThat(currentItem).isEqualTo(previousItem)
    }
}

private class ItemTrackingFakeMenu : Menu {
    var menuItems = mutableListOf<FakeMenuItem>()

    override fun add(groupId: Int, itemId: Int, order: Int, titleRes: Int): MenuItem {
        val menuItem = FakeMenuItem(testId = itemId, testOrder = order, testTitleRes = titleRes)
        menuItems.add(menuItem)
        return menuItem
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun removeItem(id: Int) {
        menuItems.removeIf { it.itemId == id }
    }

    override fun findItem(id: Int): MenuItem? {
        return menuItems.find { it.itemId == id }
    }

    override fun add(title: CharSequence?): MenuItem {
        TODO("Not yet implemented")
    }

    override fun add(titleRes: Int): MenuItem {
        TODO("Not yet implemented")
    }

    override fun add(groupId: Int, itemId: Int, order: Int, title: CharSequence?): MenuItem {
        TODO("Not yet implemented")
    }

    override fun addSubMenu(title: CharSequence?): SubMenu {
        TODO("Not yet implemented")
    }

    override fun addSubMenu(titleRes: Int): SubMenu {
        TODO("Not yet implemented")
    }

    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, title: CharSequence?): SubMenu {
        TODO("Not yet implemented")
    }

    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, titleRes: Int): SubMenu {
        TODO("Not yet implemented")
    }

    override fun addIntentOptions(
        groupId: Int,
        itemId: Int,
        order: Int,
        caller: ComponentName?,
        specifics: Array<out Intent>?,
        intent: Intent?,
        flags: Int,
        outSpecificItems: Array<out MenuItem>?
    ): Int {
        TODO("Not yet implemented")
    }

    override fun removeGroup(groupId: Int) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun setGroupCheckable(group: Int, checkable: Boolean, exclusive: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setGroupVisible(group: Int, visible: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setGroupEnabled(group: Int, enabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun hasVisibleItems(): Boolean {
        TODO("Not yet implemented")
    }

    override fun size(): Int {
        TODO("Not yet implemented")
    }

    override fun getItem(index: Int): MenuItem {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun performShortcut(keyCode: Int, event: KeyEvent?, flags: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun isShortcutKey(keyCode: Int, event: KeyEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun performIdentifierAction(id: Int, flags: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun setQwertyMode(isQwerty: Boolean) {
        TODO("Not yet implemented")
    }
}

private class FakeMenuItem(
    private val testId: Int,
    private val testOrder: Int,
    private val testTitleRes: Int
) : MenuItem {
    constructor(menuItemOption: MenuItemOption) : this(
        testId = menuItemOption.id,
        testOrder = menuItemOption.order,
        testTitleRes = menuItemOption.titleResource
    )

    override fun getItemId(): Int {
        return testId
    }

    override fun getOrder(): Int {
        return testOrder
    }

    override fun getTitle(): CharSequence {
        return testTitleRes.toString()
    }

    override fun getGroupId(): Int {
        TODO("Not yet implemented")
    }

    override fun setTitle(title: CharSequence?): MenuItem {
        TODO("Not yet implemented")
    }

    override fun setTitle(title: Int): MenuItem {
        TODO("Not yet implemented")
    }

    override fun setTitleCondensed(title: CharSequence?): MenuItem {
        TODO("Not yet implemented")
    }

    override fun getTitleCondensed(): CharSequence {
        TODO("Not yet implemented")
    }

    override fun setIcon(icon: Drawable?): MenuItem {
        TODO("Not yet implemented")
    }

    override fun setIcon(iconRes: Int): MenuItem {
        TODO("Not yet implemented")
    }

    override fun getIcon(): Drawable {
        TODO("Not yet implemented")
    }

    override fun setIntent(intent: Intent?): MenuItem {
        TODO("Not yet implemented")
    }

    override fun getIntent(): Intent {
        TODO("Not yet implemented")
    }

    override fun setShortcut(numericChar: Char, alphaChar: Char): MenuItem {
        TODO("Not yet implemented")
    }

    override fun setNumericShortcut(numericChar: Char): MenuItem {
        TODO("Not yet implemented")
    }

    override fun getNumericShortcut(): Char {
        TODO("Not yet implemented")
    }

    override fun setAlphabeticShortcut(alphaChar: Char): MenuItem {
        TODO("Not yet implemented")
    }

    override fun getAlphabeticShortcut(): Char {
        TODO("Not yet implemented")
    }

    override fun setCheckable(checkable: Boolean): MenuItem {
        TODO("Not yet implemented")
    }

    override fun isCheckable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setChecked(checked: Boolean): MenuItem {
        TODO("Not yet implemented")
    }

    override fun isChecked(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setVisible(visible: Boolean): MenuItem {
        TODO("Not yet implemented")
    }

    override fun isVisible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setEnabled(enabled: Boolean): MenuItem {
        TODO("Not yet implemented")
    }

    override fun isEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasSubMenu(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSubMenu(): SubMenu {
        TODO("Not yet implemented")
    }

    override fun setOnMenuItemClickListener(
        menuItemClickListener: MenuItem.OnMenuItemClickListener?
    ): MenuItem {
        TODO("Not yet implemented")
    }

    override fun getMenuInfo(): ContextMenu.ContextMenuInfo {
        TODO("Not yet implemented")
    }

    override fun setShowAsAction(actionEnum: Int) {
    }

    override fun setShowAsActionFlags(actionEnum: Int): MenuItem {
        TODO("Not yet implemented")
    }

    override fun setActionView(view: View?): MenuItem {
        TODO("Not yet implemented")
    }

    override fun setActionView(resId: Int): MenuItem {
        TODO("Not yet implemented")
    }

    override fun getActionView(): View {
        TODO("Not yet implemented")
    }

    override fun setActionProvider(actionProvider: ActionProvider?): MenuItem {
        TODO("Not yet implemented")
    }

    override fun getActionProvider(): ActionProvider {
        TODO("Not yet implemented")
    }

    override fun expandActionView(): Boolean {
        TODO("Not yet implemented")
    }

    override fun collapseActionView(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isActionViewExpanded(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setOnActionExpandListener(listener: MenuItem.OnActionExpandListener?): MenuItem {
        TODO("Not yet implemented")
    }
}
