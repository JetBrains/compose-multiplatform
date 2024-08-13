@file:Suppress("PrivatePropertyName")

package com.example.jetsnack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalOf
import org.jetbrains.skiko.currentNanoTime


val strsLocal = compositionLocalOf { emptyMap<Int, String>() } // intId to String
val pluralsLocal = compositionLocalOf { emptyMap<Int, PluralResource>() }

@Composable
actual fun stringResource(id: Int): String {
    return strsLocal.current[id] ?: "TODO"
}

@Composable
actual fun stringResource(id: Int, part: String): String {
    return strsLocal.current[id]?.replace("%1s", part) ?: "TODO"
}

@Composable
actual fun stringResource(id: Int, count: Int): String {
    return strsLocal.current[id]?.replace("%1d", count.toString()) ?: "TODO"
}

private var lastId = currentNanoTime().toInt()

// Filters
private var _label_filters = lastId++
actual val MppR.string.label_filters: Int get() = _label_filters

// Qty
private var _quantity = lastId++
actual val MppR.string.quantity: Int get() = _quantity

private val _label_decrease = lastId++
actual val MppR.string.label_decrease: Int get() = _label_decrease

private val _label_increase = lastId++
actual val MppR.string.label_increase: Int get() = _label_increase


// Snack detail
private val _label_back = lastId++
actual val MppR.string.label_back: Int get() = _label_back

private val _detail_header = lastId++
actual val MppR.string.detail_header: Int get() = _detail_header

private val _detail_placeholder = lastId++
actual val MppR.string.detail_placeholder: Int get() = _detail_placeholder

private val _see_more = lastId++
actual val MppR.string.see_more: Int get() = _see_more

private val _see_less = lastId++
actual val MppR.string.see_less: Int get() = _see_less

private val _ingredients = lastId++
actual val MppR.string.ingredients: Int get() = _ingredients

private val _ingredients_list = lastId++
actual val MppR.string.ingredients_list: Int get() = _ingredients_list

private val _add_to_cart = lastId++
actual val MppR.string.add_to_cart: Int get() = _add_to_cart

// Home
private val _label_select_delivery = lastId++
actual val MppR.string.label_select_delivery: Int get() = _label_select_delivery


// Filter
private val _max_calories = lastId++
actual val MppR.string.max_calories: Int get() = _max_calories

private val _per_serving = lastId++
actual val MppR.string.per_serving: Int get() = _per_serving

private val _sort = lastId++
actual val MppR.string.sort: Int get() = _sort

private val _lifestyle = lastId++
actual val MppR.string.lifestyle: Int get() = _lifestyle

private val _category = lastId++
actual val MppR.string.category: Int get() = _category

private val _price = lastId++
actual val MppR.string.price: Int get() = _price

private val _reset = lastId++
actual val MppR.string.reset: Int get() = _reset

private val _close = lastId++
actual val MppR.string.close: Int get() = _close

// Profile

private val _work_in_progress = lastId++
actual val MppR.string.work_in_progress: Int get() = _work_in_progress

private val _grab_beverage = lastId++
actual val MppR.string.grab_beverage: Int get() = _grab_beverage

// Home
private val _home_feed = lastId++
actual val MppR.string.home_feed: Int get() = _home_feed

private val _home_search = lastId++
actual val MppR.string.home_search: Int get() = _home_search

private val _home_cart = lastId++
actual val MppR.string.home_cart: Int get() = _home_cart

private val _home_profile = lastId++
actual val MppR.string.home_profile: Int get() = _home_profile


// Search
private val _search_no_matches = lastId++
actual val MppR.string.search_no_matches: Int get() = _search_no_matches

private val _search_no_matches_retry = lastId++
actual val MppR.string.search_no_matches_retry: Int get() = _search_no_matches_retry

private val _label_add = lastId++
actual val MppR.string.label_add: Int get() = _label_add

private val _search_count = lastId++
actual val MppR.string.search_count: Int get() = _search_count

private val _label_search = lastId++
actual val MppR.string.label_search: Int get() = _label_search

private val _search_jetsnack = lastId++
actual val MppR.string.search_jetsnack: Int get() = _search_jetsnack

private val _cart_increase_error = lastId++
actual val MppR.string.cart_increase_error: Int get() = _cart_increase_error

private val _cart_decrease_error = lastId++
actual val MppR.string.cart_decrease_error: Int get() = _cart_decrease_error


// Cart

private val _cart_order_count = lastId++
actual val MppR.plurals.cart_order_count: Int get() = _cart_order_count

private val _cart_order_header = lastId++
actual val MppR.string.cart_order_header: Int get() = _cart_order_header

private val _remove_item = lastId++
actual val MppR.string.remove_item: Int get() = _remove_item

private val _cart_summary_header = lastId++
actual val MppR.string.cart_summary_header: Int get() = _cart_summary_header

private val _cart_subtotal_label = lastId++
actual val MppR.string.cart_subtotal_label: Int get() = _cart_subtotal_label

private val _cart_shipping_label = lastId++
actual val MppR.string.cart_shipping_label: Int get() = _cart_shipping_label

private val _cart_total_label = lastId++
actual val MppR.string.cart_total_label: Int get() = _cart_total_label

private val _cart_checkout = lastId++
actual val MppR.string.cart_checkout: Int get() = _cart_checkout

private val _label_remove = lastId++
actual val MppR.string.label_remove: Int get() = _label_remove