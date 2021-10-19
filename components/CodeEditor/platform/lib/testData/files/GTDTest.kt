class Sum(val sum: Int) {
    val add = 4
    fun add(a: Int): Sum = Sum(add + a)
}

fun sum(a: Int, b: Int) = Sum(a + b)

fun main() {
    val localVal = 4
    var localVar = 5

    localVar = localVal

    val sum1 = sum(localVal, localVar)
    val sum2 = Sum(4)
    val sum3 = sum1.add(sum2.sum + sum1.add)

    val op1 = Ops()
    val op2 = Ops()
    op1..op2
    op1[0]
    op1(localVal)
}

class Ops {
    operator fun rangeTo(o: Ops) {}
    operator fun get(i: Int) {}
    operator fun invoke(i: Int) {}
}

// (class self) 9 0 (6 9)
// (class) 64 6 (63 66)
// (class constructor) 70 9 (69 72)
// (class val) 74 34 (73 76)
// (class fun parameter) 80 54 (79 80)
// (fun self sum) 92 0 (89 92)
// (class constructor) 112 9 (111 114)
// (fun parameter) 116 93 (115 116)
// (fun parameter) 120 101 (119 120)
// (fun self main) 131 0 (127 131)
// (loval val self) 149 0 (144 152)
// (loval val self) 152 0 (144 152)
// (local var) 191 165 (183 191)
// (local val) 200 144 (194 202)
// (fun) 221 89 (219 222)
// (local val) 226 144 (223 231)
// (local var) 238 165 (233 241)
// (class constructor) 259 9 (258 261)
// (local sum1) 282 212 (280 284)
// (class fun) 287 50 (285 288)
// (local sum2) 291 251 (289 293)
// (class val) 295 14 (294 297)
// (local sum1) 302 212 (300 304)
// (class val) 307 34 (305 308)
// (local op1) 358 319 (355 358)
// (op range) 359 425 (358 360)
// (local op2) 360 339 (360 363)
// (local op1) 371 319 (368 371)
// (op get) 374 461 (373 374)
// (local op1) 382 319 (379 382)
// (local val) 383 144 (383 391)
// (local val) 391 144 (383 391)
// (op invoke) 392 493 (391 392)
