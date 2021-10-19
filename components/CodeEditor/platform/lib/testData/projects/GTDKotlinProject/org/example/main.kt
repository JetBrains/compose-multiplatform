package org.example

import Sum
import d
import sum

fun main() {
    val sum1 = sum(1, 2)
    val sum2: Sum = Sum(4)
    val sum3 = sum1.add(sum2.sum + sum1.add) { b ->
        d(b)
    }
}

// (import Sum) 29 (Sum.kt) 6
// (import d) 39 (Sum.kt) 147
// (import sum) 49 (Sum.kt) 109
// (local val self sum1) 75 () 0
// (fun sum) 83 (Sum.kt) 109
// (class) 106 (Sum.kt) 6
// (class constructor) 112 (Sum.kt) 9
// (local val sum1) 135 (main.kt) 74
// (class fun add) 140 (Sum.kt) 50
// (local val sum2) 145 (main.kt) 99
// (class val sum) 148 (Sum.kt) 14
// (local val sum1) 155 (main.kt) 74
// (class val add) 160 (Sum.kt) 34
// (fun d) 178 (Sum.kt) 147
// (closure parameter b) 180 (main.kt) 165
