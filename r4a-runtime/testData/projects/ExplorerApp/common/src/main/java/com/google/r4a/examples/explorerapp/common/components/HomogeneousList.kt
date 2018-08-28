package com.google.r4a.examples.explorerapp.common.components

import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.google.r4a.Component
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.HierarchicalThing

/**
 * TODO(lmr): Once we can properly use generics with KTX, we should make this
 * generic
 */
class HomogeneousList<T>(comparator: DiffUtil.ItemCallback<T>) : Component() {
    private val adapter = HeaderFooterListAdapter(comparator)

    // TODO(lmr): I think the way I am doing get/set with all of these
    // properties would break Frames. Not sure the right way to do this. Perhaps we
    // should represent this list as a View and not a Component
    fun setData(data: List<T>?) {
        adapter.submitList(data)
    }
    @Children
    fun setChildren(children: @Composable() (T, Int) -> Unit) {
        adapter.children = children
    }
    @Children
    fun setChildren(children: @Composable() (T) -> Unit) {
        val children2: (T) -> Unit = children
        adapter.children = { data, _ -> children2(data) }
    }

    fun setComposeHeader(composeHeader: @Composable() (Int) -> Unit) {
        adapter.composeHeader = composeHeader
    }

    fun setComposeFooter(composeFooter: @Composable() (Int) -> Unit) {
        adapter.composeFooter = composeFooter
    }

    fun setComposeLoadingRow(composeLoadingRow: @Composable() (Int) -> Unit) {
        adapter.composeLoadingRow = composeLoadingRow
    }

    fun setHeaderCount(headerCount: Int) {
        adapter.headerCount = headerCount
    }
    fun setGetHeaderCount(getHeaderCount: () -> Int) {
        adapter.getHeaderCount = getHeaderCount
    }

    fun setFooterCount(footerCount: Int) {
        adapter.footerCount = footerCount
    }
    fun setGetFooterCount(getFooterCount: () -> Int) {
        adapter.getFooterCount = getFooterCount
    }

    fun setLoadingRowCount(loadingRowCount: Int) {
        adapter.loadingRowCount = loadingRowCount
    }
    fun setGetLoadingRowCount(getLoadingRowCount: () -> Int) {
        adapter.getLoadingRowCount = getLoadingRowCount
    }

    fun setOnLoadAround(onLoadAround: (Int) -> Unit) {
        adapter.onLoadAround = onLoadAround
    }

    var layoutParams: ViewGroup.LayoutParams? = null
    var backgroundColor: Int? = null
    var paddingTop: Dimension = 0.px


    // TODO(lmr): This is another red flag... we shouldn't be using the current context anywhere.
    private val layoutManager = LinearLayoutManager(CompositionContext.current.context).apply {
        orientation = LinearLayoutManager.VERTICAL
    }

    override fun compose() {
        with(CompositionContext.current) {
            portal(0) { ref ->
                adapter.reference = ref
                // NOTE(lmr): Realistically, we should call notifyDataSetChanged() on every compose. Otherwise, there
                // may be updates that some of the items of the recycler view. This ideally should be cheap if things
                // are working correctly, as it will essentially call "recompose()" on only the items that are in the
                // window which is what we want.
                adapter.notifyItemRangeChanged(0, adapter.itemCount)
                emitView(0, ::RecyclerView) {
                    set(adapter) { adapter = it }
                    set(layoutManager) { layoutManager = it }
                    set(paddingTop) { setPaddingTop(it) }
                    val layoutParams = layoutParams
                    if (layoutParams != null) {
                        set(layoutParams) { this.layoutParams = it }
                    }
                    // TODO(lmr): this is problematic with making components composable. I think this
                    // goes away if we make this a View instead of a Component, but handling this differently
                    // is something to consider.
                    val backgroundColor = backgroundColor
                    if (backgroundColor != null) {
                        set(backgroundColor) { this.setBackgroundColor(it) }
                    }
                }
            }
        }
    }
}

