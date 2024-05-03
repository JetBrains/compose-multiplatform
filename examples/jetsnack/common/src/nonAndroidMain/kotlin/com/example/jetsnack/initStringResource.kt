package com.example.jetsnack

fun buildStingsResources(): Map<Int, String> {
    val strs = mutableMapOf<Int, String>()
    val rs = MppR.string

    strs[rs.label_filters] = "Filters"
    strs[rs.quantity] = "Qty"
    strs[rs.label_decrease] = "Decrease"
    strs[rs.label_increase] = "Increase"

    strs[rs.label_back] = "Back"
    strs[rs.detail_header] = "Details"
    strs[rs.detail_placeholder] = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut tempus, sem vitae convallis imperdiet, lectus nunc pharetra diam, ac rhoncus quam eros eu risus. Nulla pulvinar condimentum erat, pulvinar tempus turpis blandit ut. Etiam sed ipsum sed lacus eleifend hendrerit eu quis quam. Etiam ligula eros, finibus vestibulum tortor ac, ultrices accumsan dolor. Vivamus vel nisl a libero lobortis posuere. Aenean facilisis nibh vel ultrices bibendum. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Suspendisse ac est vitae lacus commodo efficitur at ut massa. Etiam vestibulum sit amet sapien sed varius. Aliquam non ipsum imperdiet, pulvinar enim nec, mollis risus. Fusce id tincidunt nisl."
    strs[rs.see_more] = "SEE MORE"
    strs[rs.see_less] = "SEE LESS"
    strs[rs.ingredients] = "Ingredients"
    strs[rs.ingredients_list] = "Vanilla, Almond Flour, Eggs, Butter, Cream, Sugar"
    strs[rs.add_to_cart] = "ADD TO CART"
    strs[rs.label_select_delivery] = "Select delivery address"

    strs[rs.max_calories] = "Max Calories"
    strs[rs.per_serving] = "per serving"
    strs[rs.sort] = "Sort"
    strs[rs.lifestyle] = "Lifestyle"
    strs[rs.category] = "Category"
    strs[rs.price] = "Price"
    strs[rs.reset] = "Reset"
    strs[rs.close] = "Close"


    strs[rs.work_in_progress] = "This is currently work in progress"
    strs[rs.grab_beverage] = "Grab a beverage and check back later!"

    strs[rs.home_feed] = "Home"
    strs[rs.home_search] = "Search"
    strs[rs.home_cart] = "My Cart"
    strs[rs.home_profile] = "Profile"


    strs[rs.search_no_matches] = "No matches for “%1s”"
    strs[rs.search_no_matches_retry] = "Try broadening your search"
    strs[rs.label_add] = "Add to cart"
    strs[rs.search_count] = "%1d items"
    strs[rs.label_search] = "Perform search"
    strs[rs.search_jetsnack] = "Search Jetsnack"
    strs[rs.cart_increase_error] = "There was an error and the quantity couldn\\'t be increased. Please try again."
    strs[rs.cart_increase_error] = "There was an error and the quantity couldn\\'t be decreased. Please try again."

    // Cart
    strs[rs.cart_order_header] = "Order (%1s)"
    strs[rs.remove_item] = "Remove Item"
    strs[rs.cart_summary_header] = "Summary"
    strs[rs.cart_subtotal_label] = "Subtotal"
    strs[rs.cart_shipping_label] = "Shipping & Handling"
    strs[rs.cart_total_label] = "Total"
    strs[rs.cart_checkout] = "Checkout"
    strs[rs.label_remove] = "Remove item"

    return strs
}

class PluralResource(val items: Map<String, String>) {

    // TODO: this is very dumb implementation, which works only for `one` or `other`
    fun forQuantity(qty: Int): String {
        return when (qty) {
            1 -> items["one"] ?: "?????"
            else -> items["other"] ?: "?????"
        }
    }
}

fun buildPluralResources(): Map<Int, PluralResource> {
    val plurals = mutableMapOf<Int, PluralResource>()
    val ps = MppR.plurals

    plurals[ps.cart_order_count] = PluralResource(buildMap {
        this["one"] = "%1d item"
        this["other"] = "%1d items"
    })

    return plurals
}