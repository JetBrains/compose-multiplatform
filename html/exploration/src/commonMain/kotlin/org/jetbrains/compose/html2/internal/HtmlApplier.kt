package org.jetbrains.compose.html2.internal

import androidx.compose.runtime.AbstractApplier

internal class HtmlApplier(
    root: HtmlApplierNodeWrapper
) : AbstractApplier<HtmlApplierNodeWrapper>(root) {

    override fun insertTopDown(index: Int, instance: HtmlApplierNodeWrapper) {
        // ignored. Building tree bottom-up
    }

    override fun insertBottomUp(index: Int, instance: HtmlApplierNodeWrapper) {
        current.insert(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        root.clear()
    }
}
interface HtmlApplierNodeWrapper {
    fun insert(index: Int, nodeWrapper: HtmlApplierNodeWrapper)
    fun remove(index: Int, count: Int)
    fun move(from: Int, to: Int, count: Int)
    fun clear()
}