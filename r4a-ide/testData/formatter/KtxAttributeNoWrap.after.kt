// SET_INT: CALL_PARAMETERS_WRAP = 1
// SET_TRUE: ALIGN_MULTILINE_PARAMETERS_IN_CALLS
// RIGHT_MARGIN: 5

fun ktx() {
    <LinearLayout orientation="horizontal">
        <Button text="foo" onClick={ doSomething(1, 2, 3, 4, 5, 6, 7, 8, 9) } />
        values("Apples",
               "Oranges",
               "Grapes",
               "Bananas")
    </LinearLayout>
}