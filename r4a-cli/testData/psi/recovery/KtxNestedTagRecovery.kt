
fun unclosedTags() {
    <LinearLayout>
        <Closed1>
            <Unclosed1>
                <Unclosed2>
        </Closed1>
        <NextElementToVerifyDepth />
    </LinearLayout>
}

fun unopenedCloses() {
    <LinearLayout>
        </UnopenedClosed>
        </UnopenedClosed>
        </UnopenedClosed>
        <NextElementToVerifyDepth />
    </LinearLayout>
}
