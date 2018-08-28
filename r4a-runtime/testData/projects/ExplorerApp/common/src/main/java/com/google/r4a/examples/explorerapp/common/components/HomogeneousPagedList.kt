package com.google.r4a.examples.explorerapp.common.components

import android.arch.paging.PagedList
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.google.r4a.*
import com.google.r4a.CompositionContext
import com.google.r4a.adapters.Dimension
import com.google.r4a.adapters.px
import com.google.r4a.adapters.setPaddingTop
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.Link

// TODO(lmr): refactor this when we codegen type-parameter calls correctly
class HomogeneousPagedList<T>(
    comparator: DiffUtil.ItemCallback<T>
) : Component() {
    private val adapter = HeaderFooterPagedListAdapter(comparator)

    // TODO(lmr): I think the way I am doing get/set with all of these
    // properties would break Frames. Not sure the right way to do this. Perhaps we
    // should represent this list as a View and not a Component
    fun setData(data: PagedList<T>?) {
        adapter.submitList(data)
    }
    @Children
    fun setChildren(children: @Composable() (T, Int) -> Unit) {
        adapter.children = children
    }
    @Children
    fun setChildren(children: @Composable() (T) -> Unit) {
        val children2: (T) -> Unit = children;
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

    var paddingTop: Dimension = 0.px

    var layoutParams: ViewGroup.LayoutParams? = null
    var backgroundColor: Int? = null


    private val layoutManager = LinearLayoutManager(CompositionContext.current.context).apply {
        orientation = LinearLayoutManager.VERTICAL
    }

    override fun compose() {
        with(CompositionContext.current) {
            portal(0) { ref ->
                adapter.reference = ref
                emitView(0, ::RecyclerView) {
                    el.isNestedScrollingEnabled = true
                    set(adapter) { adapter = it }
                    set(layoutManager) { layoutManager = it }
                    set(paddingTop) { setPaddingTop(it) }
//                    set(it, true) { setHasFixedSize(it) }
                    val layoutParams = layoutParams
                    if (layoutParams != null) {
                        set(layoutParams) { this.layoutParams = it }
                    }
                    val backgroundColor = backgroundColor
                    if (backgroundColor != null) {
                        set(backgroundColor) { this.setBackgroundColor(it) }
                    }
                }
            }
        }
    }
}


