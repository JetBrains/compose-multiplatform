package com.google.r4a.components

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.r4a.composeInto

class Recycler(context: Context): RecyclerView(context) {

    lateinit var getItemCount: () -> Int
    var composeItem: ((position: Int) -> Unit)? = null
    var composeItemWithType: ((position: Int, type: Int) -> Unit)? = null
    var getItemViewType: ((Int) -> Int)? = null

    class TypedViewHolder(val viewType: Int, context: Context): RecyclerView.ViewHolder(LinearLayout(context))

    @Suppress("PLUGIN_WARNING")
    private val myAdapter = object: RecyclerView.Adapter<TypedViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypedViewHolder {
            return TypedViewHolder(viewType, parent.getContext())
        }

        override fun getItemCount(): Int {
            return this@Recycler.getItemCount()
        }

        override fun onBindViewHolder(holder: TypedViewHolder, position: Int) {
            val view = holder.itemView as LinearLayout
            val viewType = holder.viewType

            view.composeInto({
                val composeItemWithType = composeItemWithType
                val composeItem = composeItem
                if (composeItemWithType != null) {
                    <composeItemWithType position type={viewType} />
                }
                else if (composeItem != null) {
                    <composeItem position />
                } else "Foo" // TODO: Remove this, needed to prevent compile errors due to IR bug.
            })
        }

        override fun getItemViewType(position: Int): Int {
            val fn = getItemViewType
            return if (fn != null) fn(position) else super.getItemViewType(position)
        }
    }

    init {
        setAdapter(myAdapter)
    }
}