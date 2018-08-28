package com.google.r4a.examples.explorerapp.common.components

import android.support.v7.recyclerview.extensions.AsyncDifferConfig
import android.support.v7.recyclerview.extensions.AsyncListDiffer
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.r4a.*
import com.google.r4a.CompositionContext

/**
 * This class is a RecyclerView Adapter that is meant specifically to be used with R4A. It works a lot like the
 * existing ListAdapter, except that it has some special handling for the use case where you have a list of
 * "items", but also want to use the RecyclerView to display other parts of the UI, such as a "Header",
 * "Footer", and also rows that are used as a "loading" state. This is a really common use case and typically
 * is handled manually, but this complicates the logic with dealing with lists a lot, so it's nice if we
 * can just abstract it here, once, properly.
 *
 * TODO(lmr): This shares a lot of code with HeaderFooterPagedListAdapter. Perhaps find out how to share
 */
class HeaderFooterListAdapter<T>(callback: DiffUtil.ItemCallback<T>) : RecyclerView.Adapter<XViewHolder>() {

    /**
     * Our ListUpdateCallback is used for "items" only, which come after the header rows, so we have
     * to offset all of these calls by getHeaderCount()
     */
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

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            notifyItemRangeChanged(getHeaderCount() + position, count, payload)
        }
    }

    /**
     * We build a differ for the items only
     */
    private val differ = AsyncListDiffer(
            listUpdateCallback,
            AsyncDifferConfig.Builder(callback).build()
    )

    fun submitList(list: List<T>?) = differ.submitList(list)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val TYPE_LOADING = 2
        private const val TYPE_FOOTER = 3
    }

    // TODO(lmr): we end up requiring a reference here, but if we move to an Ambient via Context model, we
    // might be able to get rid of it.
    lateinit var reference: Ambient.Reference

    var headerCount: Int = 0
        set(value) {
            val prev = field
            field = value
            onSomeCountChanged(prev, value, 0)
        }
    // TODO(lmr): handle changes to these props properties properly
    var getHeaderCount: () -> Int = { headerCount }
    var composeHeader: (Int) -> Unit = {}

    var footerCount: Int = 0
        set(value) { field = value; notifyDataSetChanged() }
    // TODO(lmr): handle changes to these props properties properly
    var getFooterCount: () -> Int = { footerCount }
    var composeFooter: (Int) -> Unit = {}

    var loadingRowCount: Int = 0
        set(value) {
            val prev = field
            field = value
            onSomeCountChanged(prev, value, getHeaderCount() + getJustItemCount())
        }
    // TODO(lmr): handle changes to these props properties properly
    var getLoadingRowCount: () -> Int = { loadingRowCount }
    var composeLoadingRow: (Int) -> Unit = {}


    // TODO(lmr): handle changes to these props properties properly
    var children: (T, Int) -> Unit = { _, _ -> }
        set(value) {
            field = value
            notifyItemRangeChanged(getHeaderCount(), getJustItemCount())
        }

    /**
     * This callback is called when an item around a certain position is used. This is a callback which can
     * be used to handle infinite scrolling and paging
     */
    var onLoadAround: (Int) -> Unit = {}


    /**
     * If the number of headers/footers/loaders etc change, we use this callback to properly notify
     * the adapter
     */
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
            // this is what PagedListAdapter does that we are going to emulate...
            onLoadAround(realPosition)
            val item = differ.currentList[realPosition] ?: error("couldn't find item")
            reference.composeInto(view) {
                with(CompositionContext.current) {
                    group(0) { children(item, realPosition) }
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
                    group(0) { composeLoadingRow(realPosition) }
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
                    group(0) { composeFooter(realPosition) }
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

    private fun getJustItemCount() = differ.currentList.size

    override fun getItemCount(): Int {
        return getJustItemCount() + getHeaderCount() + getFooterCount() + getLoadingRowCount()
    }

}