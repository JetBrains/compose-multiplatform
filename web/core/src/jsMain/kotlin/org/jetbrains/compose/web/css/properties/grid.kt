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


