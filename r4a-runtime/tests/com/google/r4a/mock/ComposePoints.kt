package com.google.r4a.mock

fun ViewComposition.point(point: Point) {
    text("X: ${point.x} Y: ${point.y}")
}

fun ViewValidator.point(point: Point) {
    text("X: ${point.x} Y: ${point.y}")
}

object SLPoints

fun ViewComposition.points(points: Iterable<Point>) {
    repeat(of = points) {
        memoize(SLPoints, it) { point(it) }
    }
}

fun ViewValidator.points(points: Iterable<Point>) {
    repeat(of = points) {
        point(it)
    }
}
