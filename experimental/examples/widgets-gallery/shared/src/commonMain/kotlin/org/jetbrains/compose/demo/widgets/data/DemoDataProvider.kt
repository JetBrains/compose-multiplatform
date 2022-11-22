package org.jetbrains.compose.demo.widgets.data

import org.jetbrains.compose.demo.widgets.data.model.Item
import org.jetbrains.compose.demo.widgets.data.model.Tweet
import org.jetbrains.compose.demo.widgets.platform.Res

object DemoDataProvider {
    val item = Item(
        1,
        "Awesome List Item",
        "Very awesome list item has very awesome subtitle. This is bit long",
        Res.drawable.food6
    )
}