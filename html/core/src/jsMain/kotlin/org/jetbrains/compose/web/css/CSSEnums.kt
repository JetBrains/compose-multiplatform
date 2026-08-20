@file:Suppress("Unused", "NOTHING_TO_INLINE", "NESTED_CLASS_IN_EXTERNAL_INTERFACE", "INLINE_EXTERNAL_DECLARATION", "WRONG_BODY_OF_EXTERNAL_DECLARATION", "NESTED_EXTERNAL_DECLARATION")

package org.jetbrains.compose.web.css

object GridAutoFlow : StylePropertyString  {
    inline val Row get() = "row".unsafeCast<GridAutoFlow>()
    inline val Column get() = "column".unsafeCast<GridAutoFlow>()
    inline val Dense get() = "dense".unsafeCast<GridAutoFlow>()
    inline val RowDense get() = "row dense".unsafeCast<GridAutoFlow>()
    inline val ColumnDense get() = "column dense".unsafeCast<GridAutoFlow>()
}
