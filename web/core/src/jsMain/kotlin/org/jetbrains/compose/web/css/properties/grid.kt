/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-column
fun StyleBuilder.gridColumn(value: String) {
    property("grid-column", value)
}

fun StyleBuilder.gridColumn(start: String, end: String) {
    property("grid-column", "$start / $end")
}

fun StyleBuilder.gridColumn(start: String, end: Int) {
    property("grid-column", "$start / $end")
}

fun StyleBuilder.gridColumn(start: Int, end: String) {
    property("grid-column", "$start / $end")
}

fun StyleBuilder.gridColumn(start: Int, end: Int) {
    property("grid-column", "$start / $end")
}


// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-column-start
fun StyleBuilder.gridColumnStart(value: String) {
    property("grid-column-start", value)
}

fun StyleBuilder.gridColumnStart(value: Int) {
    property("grid-column-start", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-column-end
fun StyleBuilder.gridColumnEnd(value: String) {
    property("grid-column-end", value)
}

fun StyleBuilder.gridColumnEnd(value: Int) {
    property("grid-column-end", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-row
fun StyleBuilder.gridRow(value: String) {
    property("grid-row", value)
}

fun StyleBuilder.gridRow(start: String, end: String) {
    property("grid-row", "$start / $end")
}

fun StyleBuilder.gridRow(start: String, end: Int) {
    property("grid-row", "$start / $end")
}

fun StyleBuilder.gridRow(start: Int, end: String) {
    property("grid-row", "$start / $end")
}

fun StyleBuilder.gridRow(start: Int, end: Int) {
    property("grid-row", "$start / $end")
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-row-start
fun StyleBuilder.gridRowStart(value: String) {
    property("grid-row-start", value)
}

fun StyleBuilder.gridRowStart(value: Int) {
    property("grid-row-start", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-row-end
fun StyleBuilder.gridRowEnd(value: String) {
    property("grid-row-end", value)
}

fun StyleBuilder.gridRowEnd(value: Int) {
    property("grid-row-end", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-columns
fun StyleBuilder.gridTemplateColumns(value: String) {
    property("grid-template-columns", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-rows
fun StyleBuilder.gridTemplateRows(value: String) {
    property("grid-template-rows", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-area
fun StyleBuilder.gridArea(rowStart: String) {
    property("grid-area", rowStart)
}

fun StyleBuilder.gridArea(rowStart: String, columnStart: String) {
    property("grid-area", "$rowStart / $columnStart")
}

fun StyleBuilder.gridArea(rowStart: String, columnStart: String, rowEnd: String) {
    property("grid-area", "$rowStart / $columnStart / $rowEnd")
}

fun StyleBuilder.gridArea(rowStart: String, columnStart: String, rowEnd: String, columnEnd: String) {
    property("grid-area", "$rowStart / $columnStart / $rowEnd / $columnEnd")
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-areas
fun StyleBuilder.gridTemplateAreas(vararg rows: String) {
    property("grid-template-areas", rows.joinToString(" ") { "\"$it\"" })
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/justify-self
fun StyleBuilder.justifySelf(value: String) {
    property("justify-self", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/justify-items
fun StyleBuilder.justifyItems(value: String) {
    property("justify-items", value)
}


// https://developer.mozilla.org/en-US/docs/Web/CSS/align-self
fun StyleBuilder.alignSelf(value: String) {
    property("align-self", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/align-items
fun StyleBuilder.alignItems(value: String) {
    property("align-items", value)
}


// https://developer.mozilla.org/en-US/docs/Web/CSS/place-self
fun StyleBuilder.placeSelf(value: String) {
    property("place-self", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/row-gap
fun StyleBuilder.rowGap(value: CSSNumeric) {
    property("row-gap", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/column-gap
fun StyleBuilder.columnGap(value: CSSNumeric) {
    property("column-gap", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/gap
fun StyleBuilder.gap(value: CSSNumeric) {
    property("gap", value)
}

fun StyleBuilder.gap(rowGap: CSSNumeric, columnGap: CSSNumeric) {
    property("gap", "$rowGap $columnGap")
}

