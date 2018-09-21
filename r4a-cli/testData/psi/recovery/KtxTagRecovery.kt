fun a0() {
    <
}

fun a1() {
    <Bar
}

fun a2() {
    <com.foo.
}

fun a3() {
    <Bar attribute
}

fun a4() {
    <Bar attribute=
}

fun a5() {
    <Bar attribute=
    println("Some other statement, to break attribute expression");
}

fun a6() {
    <Bar attribute=()
}

fun a7() {
    <Bar attribute="
}

fun a8() {
    <Bar>
}

fun a9() {
    <Bar><
}

fun a10() {
    <Bar></
}

fun a11() {
    <Bar></Bar
}

fun a12() {
    <com.foo.Noise></com.foo.
}

fun a13() {
    lambda {
        <Bar attribute=val />
    }
}

fun a14() {
    <Hello world$ foo=123 />
}
