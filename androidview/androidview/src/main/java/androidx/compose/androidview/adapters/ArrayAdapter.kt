/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.androidview.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

// TODO(lmr): This should be moved to a separate module, but needs to be one that is not IR-compiled
class ArrayAdapter<T> : BaseAdapter(), Filterable {
    var composable: (@Composable (T) -> Unit)? = null
    var items: MutableList<T>? = null
    var itemLayoutResourceId: Int = android.R.layout.simple_list_item_1
    var itemFieldId: Int = 0

    private val lock = object {}
    private var originalValues: ArrayList<T>? = null
    private val filter: ArrayFilter by lazy { ArrayFilter() }
    private var inflater: LayoutInflater? = null

    override fun getFilter(): Filter = filter

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        if (parent == null) error("Expected non-null parent")
        return if (composable != null)
            getViewFromComposable(position, convertView, parent)
        else
            getViewFromLayout(position, convertView, parent)
    }

    private fun getViewFromLayout(position: Int, convertView: View?, parent: ViewGroup): View {
        var inflater = inflater
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)!!
            this.inflater = inflater
        }
        return createViewFromResource(inflater, position, convertView, parent, itemLayoutResourceId)
    }

    private fun createViewFromResource(
        inflater: LayoutInflater,
        position: Int,
        convertView: View?,
        parent: ViewGroup,
        resource: Int
    ): View {
        val context = parent.context
        val text: TextView?

        val view = convertView ?: inflater.inflate(resource, parent, false)

        try {
            if (itemFieldId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = view as TextView
            } else {
                //  Otherwise, find the TextView field within the layout
                text = view.findViewById(itemFieldId) as? TextView

                if (text == null) {
                    throw RuntimeException(
                        "Failed to find view with ID ${
                        context.resources.getResourceName(itemFieldId)
                        } in item layout"
                    )
                }
            }
        } catch (e: ClassCastException) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView")
            throw IllegalStateException(
                "ArrayAdapter requires the resource ID to be a TextView", e
            )
        }

        val item = getItem(position)
        if (item is CharSequence) {
            text.text = item
        } else {
            text.text = item.toString()
        }

        return view
    }

    @Suppress("PLUGIN_WARNING")
    private fun getViewFromComposable(position: Int, convertView: View?, parent: ViewGroup): View {
        if (composable == null) error("Expected composable to be non-null")
        val items = items ?: error("Expected non-null items array")

        val item = items[position]
        val view = convertView ?: ComposeView(parent.context)
        val group = view as ComposeView

        group.setContent {
            composable!!(item)
        }

        return view
    }

    override fun getItem(position: Int): Any {
        val items = items ?: error("Expected non-null items array")
        return items[position] as Any
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int {
        val items = items ?: return 0
        return items.size
    }

    private inner class ArrayFilter : Filter() {
        override fun performFiltering(prefix: CharSequence?): Filter.FilterResults {
            val results = Filter.FilterResults()

            if (originalValues == null) {
                originalValues = synchronized(lock) { ArrayList<T>(items!!) }
            }

            if (prefix == null || prefix.isEmpty()) {
                val list = synchronized(lock) { ArrayList<T>(originalValues!!) }
                results.values = list
                results.count = list.size
            } else {
                val prefixString = prefix.toString().lowercase()

                val values = synchronized(lock) { ArrayList<T>(originalValues!!) }

                val count = values.size
                val newValues = ArrayList<T>()

                for (i in 0 until count) {
                    val value = values[i]
                    val valueText = value.toString().lowercase()

                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(value)
                    } else {
                        val words = valueText.split(" ".toRegex()).dropLastWhile({
                            it.isEmpty()
                        }).toTypedArray()
                        for (word in words) {
                            if (word.startsWith(prefixString)) {
                                newValues.add(value)
                                break
                            }
                        }
                    }
                }

                results.values = newValues
                results.count = newValues.size
            }

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults) {
            @Suppress("UNCHECKED_CAST")
            items = results.values as MutableList<T>
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }
}