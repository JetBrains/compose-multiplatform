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

            view.composeInto(object : Function0<Unit> {
                override fun invoke() {
                    val composeItemWithType = composeItemWithType
                    if (composeItemWithType != null) {
                        <composeItemWithType arg0={position} arg1={viewType} />
                        return
                    }
                    val composeItem = composeItem
                    if (composeItem != null) {
                        <composeItem arg0={position} />
                        return
                    }
                }
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