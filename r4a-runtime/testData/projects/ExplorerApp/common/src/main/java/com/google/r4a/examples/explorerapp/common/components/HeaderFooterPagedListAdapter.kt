package com.google.r4a.examples.explorerapp.common.components

import android.arch.paging.AsyncPagedListDiffer
import android.arch.paging.PagedList
import android.arch.paging.PagedListAdapter
import android.support.v7.recyclerview.extensions.AsyncDifferConfig
import android.support.v7.recyclerview.extensions.AsyncListDiffer
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.r4a.Ambient
import com.google.r4a.*

class HeaderFooterPagedListAdapter<T>(callback: DiffUtil.ItemCallback<T>) : RecyclerView.Adapter<XViewHolder>() {


    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(getHeaderCount() + position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(getHeaderCount() + position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(getHeaderCount() + fromPosition, getHeaderCount() + toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any) {
            notifyItemRangeChanged(getHeaderCount() + position, count, payload)
        }
    }

    private val differ = AsyncPagedListDiffer(
            listUpdateCallback,
            AsyncDifferConfig.Builder(callback).build()
    )

    fun submitList(list: PagedList<T>?) = differ.submitList(list)


    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        const val TYPE_LOADING = 2
        const val TYPE_FOOTER = 3
    }

    lateinit var reference: Ambient.Reference

    var headerCount: Int = 0
        set(value) {
            val prev = field
            field = value
            onSomeCountChanged(prev, value, 0)
        }
    var getHeaderCount: () -> Int = { headerCount }
    var composeHeader: (Int) -> Unit = {}
        set(value) {
            field = value
            notifyItemRangeChanged(0, getHeaderCount())
        }

    var footerCount: Int = 0
        set(value) {
            val prev = field
            field = value
            onSomeCountChanged(prev, value, getHeaderCount() + getJustItemCount() + getLoadingRowCount())
        }
    var getFooterCount: () -> Int = { footerCount }
    var composeFooter: (Int) -> Unit = {}

    var loadingRowCount: Int = 0
        set(value) {
            val prev = field
            field = value
            onSomeCountChanged(prev, value, getHeaderCount() + getJustItemCount())
        }
    var getLoadingRowCount: () -> Int = { loadingRowCount }
    var composeLoadingRow: (Int) -> Unit = {}


    var children: (T, Int) -> Unit = { _, _ -> }

    private fun onSomeCountChanged(prev: Int, value: Int, offset: Int) {
        if (prev < value) {
            notifyItemRangeInserted(offset + prev, value - prev)
        } else if (prev > value) {
            notifyItemRangeRemoved(offset + value, prev - value)
        }
    }


    override fun onBindViewHolder(holder: XViewHolder, position: Int) {
        val view = holder.itemView as LinearLayout
        val viewType = holder.viewType
        var index = position

        // header
        if (viewType == TYPE_HEADER) {
            val realPosition = index
            reference.composeInto(view) {
                with(CompositionContext.current) {
                    group(0) {
                        composeHeader(realPosition)
                    }
                }
            }
            return
        } else {
            index -= headerCount
        }

        // items
        if (viewType == TYPE_ITEM) {
            val realPosition = index
            val item = differ.getItem(realPosition) ?: error("couldn't find item")
            reference.composeInto(view) {
                with(CompositionContext.current) {
                    group(0) {
                        children(item, realPosition)
                    }
                }
            }
            return
        } else {
            index -= itemCount
        }

        // loading rows
        if (viewType == TYPE_LOADING) {
            val realPosition = index
            reference.composeInto(view) {
                with(CompositionContext.current) {
                    group(0) {
                        composeLoadingRow(realPosition)
                    }
                }
            }
            return
        } else {
            index -= loadingRowCount
        }

        // footer rows
        if (viewType == TYPE_FOOTER) {
            val realPosition = index
            reference.composeInto(view) {
                with(CompositionContext.current) {
                    group(0) {
                        composeFooter(realPosition)
                    }
                }
            }
            return
        } else {
            index -= footerCount
        }

        error("Unrecognized view type!")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = XViewHolder(parent, viewType)

    override fun getItemViewType(position: Int): Int {
        val headerCount = getHeaderCount()
        val itemCount = getJustItemCount()
        val loadingRowCount = getLoadingRowCount()
        val footerCount = getFooterCount()

        var index = position

        // header
        if (index < headerCount) {
            return TYPE_HEADER
        } else {
            index -= headerCount
        }

        // items
        if (index < itemCount) {
            return TYPE_ITEM
        } else {
            index -= itemCount
        }

        // loading rows
        if (index < loadingRowCount) {
            return TYPE_LOADING
        } else {
            index -= loadingRowCount
        }

        // footer rows
        if (index < footerCount) {
            return TYPE_FOOTER
        } else {
            index -= footerCount
        }

        error("Unexpected position: $position")
    }

    private fun getJustItemCount() = differ.itemCount

    override fun getItemCount(): Int {
        return getJustItemCount() + getHeaderCount() + getFooterCount() + getLoadingRowCount()
    }

}