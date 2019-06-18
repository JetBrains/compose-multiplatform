package androidx.compose.mock

fun MockViewComposition.point(point: Point) {
    text("X: ${point.x} Y: ${point.y}")
}

fun MockViewValidator.point(point: Point) {
    text("X: ${point.x} Y: ${point.y}")
}

object SLPoints

fun MockViewComposition.points(points: Iterable<Point>) {
    repeat(of = points) {
        memoize(SLPoints, it) { point(it) }
    }
}

fun MockViewValidator.points(points: Iterable<Point>) {
    repeat(of = points) {
        point(it)
    }
}
