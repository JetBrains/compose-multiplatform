@file:Suppress("PrivatePropertyName")

package com.example.jetsnack

import androidx.compose.runtime.Composable

@Composable
actual fun stringResource(id: Int): String {
   return androidx.compose.ui.res.stringResource(id)
}

@Composable
actual fun stringResource(id: Int, part: String): String {
    return androidx.compose.ui.res.stringResource(id, part)
}

@Composable
actual fun stringResource(id: Int, count: Int): String {
    return androidx.compose.ui.res.stringResource(id, count)
}


// Filters
actual val MppR.string.label_filters: Int get() = R.string.label_filters

// Qty
actual val MppR.string.quantity: Int get() = R.string.quantity

actual val MppR.string.label_decrease: Int get() = R.string.label_decrease

actual val MppR.string.label_increase: Int get() = R.string.label_increase


// Snack detail
actual val MppR.string.label_back: Int get() = R.string.label_back

actual val MppR.string.detail_header: Int get() = R.string.detail_header

actual val MppR.string.detail_placeholder: Int get() = R.string.detail_placeholder

actual val MppR.string.see_more: Int get() = R.string.see_more

actual val MppR.string.see_less: Int get() = R.string.see_less

actual val MppR.string.ingredients: Int get() = R.string.ingredients

actual val MppR.string.ingredients_list: Int get() = R.string.ingredients_list

actual val MppR.string.add_to_cart: Int get() = R.string.add_to_cart

// Home
actual val MppR.string.label_select_delivery: Int get() = R.string.label_select_delivery


// Filter
actual val MppR.string.max_calories: Int get() = R.string.max_calories

actual val MppR.string.per_serving: Int get() = R.string.per_serving

actual val MppR.string.sort: Int get() = R.string.sort

actual val MppR.string.lifestyle: Int get() = R.string.lifestyle

actual val MppR.string.category: Int get() = R.string.category

actual val MppR.string.price: Int get() = R.string.price

actual val MppR.string.reset: Int get() = R.string.reset

actual val MppR.string.close: Int get() = R.string.close

// Profile

actual val MppR.string.work_in_progress: Int get() = R.string.work_in_progress

actual val MppR.string.grab_beverage: Int get() = R.string.grab_beverage

// Home
actual val MppR.string.home_feed: Int get() = R.string.home_feed

actual val MppR.string.home_search: Int get() = R.string.home_search

actual val MppR.string.home_cart: Int get() = R.string.home_cart

actual val MppR.string.home_profile: Int get() = R.string.home_profile


// Search
actual val MppR.string.search_no_matches: Int get() = R.string.search_no_matches

actual val MppR.string.search_no_matches_retry: Int get() = R.string.search_no_matches_retry

actual val MppR.string.label_add: Int get() = R.string.label_add

actual val MppR.string.search_count: Int get() = R.string.search_count

actual val MppR.string.label_search: Int get() = R.string.label_search

actual val MppR.string.search_jetsnack: Int get() = R.string.search_jetsnack

actual val MppR.string.cart_increase_error: Int get() = R.string.cart_increase_error
actual val MppR.string.cart_decrease_error: Int get() = R.string.cart_decrease_error


// Cart
actual val MppR.plurals.cart_order_count: Int get() = R.plurals.cart_order_count
actual val MppR.string.cart_order_header: Int get() = R.string.cart_order_header
actual val MppR.string.remove_item: Int get() = R.string.remove_item
actual val MppR.string.cart_summary_header: Int get() = R.string.cart_summary_header
actual val MppR.string.cart_subtotal_label: Int get() = R.string.cart_subtotal_label
actual val MppR.string.cart_shipping_label: Int get() = R.string.cart_shipping_label
actual val MppR.string.cart_total_label: Int get() = R.string.cart_total_label
actual val MppR.string.cart_checkout: Int get() = R.string.cart_checkout
actual val MppR.string.label_remove: Int get() = R.string.label_remove