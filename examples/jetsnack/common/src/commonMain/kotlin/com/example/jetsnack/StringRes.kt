package com.example.jetsnack

import androidx.compose.runtime.Composable

@Composable
expect fun stringResource(id: Int): String

@Composable
expect fun stringResource(id: Int, part: String): String

@Composable
expect fun stringResource(id: Int, count: Int): String

object MppR {
    object string {}

    object drawable {}

    object plurals {}
}

expect val MppR.plurals.cart_order_count: Int

// Filters
expect val MppR.string.label_filters: Int

// Qty
expect val MppR.string.quantity: Int
expect val MppR.string.label_decrease: Int
expect val MppR.string.label_increase: Int


// Snack detail
expect val MppR.string.label_back: Int
expect val MppR.string.detail_header: Int
expect val MppR.string.detail_placeholder: Int
expect val MppR.string.see_more: Int
expect val MppR.string.see_less: Int
expect val MppR.string.ingredients: Int
expect val MppR.string.ingredients_list: Int
expect val MppR.string.add_to_cart: Int

// Home

expect val MppR.string.label_select_delivery: Int

// Filter
expect val MppR.string.max_calories: Int
expect val MppR.string.per_serving: Int
expect val MppR.string.sort: Int
expect val MppR.string.lifestyle: Int
expect val MppR.string.category: Int
expect val MppR.string.price: Int
expect val MppR.string.reset: Int
expect val MppR.string.close: Int

// Profile

expect val MppR.string.work_in_progress: Int
expect val MppR.string.grab_beverage: Int

// Home
expect val MppR.string.home_feed: Int
expect val MppR.string.home_search: Int
expect val MppR.string.home_cart: Int
expect val MppR.string.home_profile: Int

// Search
expect val MppR.string.search_no_matches: Int
expect val MppR.string.search_no_matches_retry: Int
expect val MppR.string.label_add: Int
expect val MppR.string.search_count: Int
expect val MppR.string.label_search: Int
expect val MppR.string.search_jetsnack: Int

expect val MppR.string.cart_increase_error: Int
expect val MppR.string.cart_decrease_error: Int

// Cart
expect val MppR.string.cart_order_header: Int
expect val MppR.string.remove_item: Int
expect val MppR.string.cart_summary_header: Int
expect val MppR.string.cart_subtotal_label: Int
expect val MppR.string.cart_shipping_label: Int
expect val MppR.string.cart_total_label: Int
expect val MppR.string.cart_checkout: Int
expect val MppR.string.label_remove: Int