/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-column
fun StyleScope.gridColumn(value: String) {
    property("grid-column", value)
}

fun StyleScope.gridColumn(start: String, end: String) {
    property("grid-column", "$start / $end")
}

fun StyleScope.gridColumn(start: String, end: Int) {
    property("grid-column", "$start / $end")
}

fun StyleScope.gridColumn(start: Int, end: String) {
    property("grid-column", "$start / $end")
}

fun StyleScope.gridColumn(start: Int, end: Int) {
    property("grid-column", "$start / $end")
}


// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-column-start
fun StyleScope.gridColumnStart(value: String) {
    property("grid-column-start", value)
}

fun StyleScope.gridColumnStart(value: Int) {
    property("grid-column-start", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-column-end
fun StyleScope.gridColumnEnd(value: String) {
    property("grid-column-end", value)
}

fun StyleScope.gridColumnEnd(value: Int) {
    property("grid-column-end", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-row
fun StyleScope.gridRow(value: String) {
    property("grid-row", value)
}

fun StyleScope.gridRow(start: String, end: String) {
    property("grid-row", "$start / $end")
}

fun StyleScope.gridRow(start: String, end: Int) {
    property("grid-row", "$start / $end")
}

fun StyleScope.gridRow(start: Int, end: String) {
    property("grid-row", "$start / $end")
}

fun StyleScope.gridRow(start: Int, end: Int) {
    property("grid-row", "$start / $end")
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-row-start
fun StyleScope.gridRowStart(value: String) {
    property("grid-row-start", value)
}

fun StyleScope.gridRowStart(value: Int) {
    property("grid-row-start", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-row-end
fun StyleScope.gridRowEnd(value: String) {
    property("grid-row-end", value)
}

fun StyleScope.gridRowEnd(value: Int) {
    property("grid-row-end", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-columns
fun StyleScope.gridTemplateColumns(value: String) {
    property("grid-template-columns", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-columns
fun StyleScope.gridAutoColumns(value: String) {
    property("grid-auto-columns", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-flow
fun StyleScope.gridAutoFlow(value: GridAutoFlow) {
    property("grid-auto-flow", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-rows
fun StyleScope.gridTemplateRows(value: String) {
    property("grid-template-rows", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-rows
fun StyleScope.gridAutoRows(value: String) {
    property("grid-auto-rows", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-area
fun StyleScope.gridArea(rowStart: String) {
    property("grid-area", rowStart)
}

fun StyleScope.gridArea(rowStart: String, columnStart: String) {
    property("grid-area", "$rowStart / $columnStart")
}

fun StyleScope.gridArea(rowStart: String, columnStart: String, rowEnd: String) {
    property("grid-area", "$rowStart / $columnStart / $rowEnd")
}

fun StyleScope.gridArea(rowStart: String, columnStart: String, rowEnd: String, columnEnd: String) {
    property("grid-area", "$rowStart / $columnStart / $rowEnd / $columnEnd")
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-areas
fun StyleScope.gridTemplateAreas(vararg rows: String) {
    property("grid-template-areas", rows.joinToString(" ") { "\"$it\"" })
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/justify-self
fun StyleScope.justifySelf(value: String) {
    property("justify-self", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/justify-items
fun StyleScope.justifyItems(value: String) {
    property("justify-items", value)
}


// https://developer.mozilla.org/en-US/docs/Web/CSS/align-self
fun StyleScope.alignSelf(value: String) {
    property("align-self", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/align-items
fun StyleScope.alignItems(value: String) {
    property("align-items", value)
}


// https://developer.mozilla.org/en-US/docs/Web/CSS/place-self
fun StyleScope.placeSelf(value: String) {
    property("place-self", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/row-gap
fun StyleScope.rowGap(value: CSSNumeric) {
    property("row-gap", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/column-gap
fun StyleScope.columnGap(value: CSSNumeric) {
    property("column-gap", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/gap
fun StyleScope.gap(value: CSSNumeric) {
    property("gap", value)
}

fun StyleScope.gap(rowGap: CSSNumeric, columnGap: CSSNumeric) {
    property("gap", "$rowGap $columnGap")
}

