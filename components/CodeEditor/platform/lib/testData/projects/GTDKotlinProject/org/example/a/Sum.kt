class Sum(val sum: Int) {
    val add = 4
    fun add(a: Int, b: (Int) -> Int): Sum = Sum(add + b(a))
}

fun sum(a: Int, b: Int) = Sum(a + b)

val d: (Int) -> Int = { b -> 2 * b }
