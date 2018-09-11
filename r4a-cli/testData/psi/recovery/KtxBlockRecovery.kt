
fun TestKtxBlockRecovery() {
    // If statement has a KTX block with no LBRACKET between `if` and KTX
    if(true)
        <LinearLayout>
            <Closed1>
                <Unclosed1>
                    <Unclosed2>
                    for(x in 1..5) {  // Valid for loop causes no problems
                        println("foo")
                    }
            </Closed1>
            <NextElementToVerifyDepth />
        </LinearLayout>
}


fun TestKtxBlockRecovery() {
    if(true)
        <LinearLayout>
            </Unopened1>
            </Unopened2>
        </LinearLayout>
}

fun TestKtxBlockWithLeadingComment() {
    if(true)  // EOL Comment between `if` and KTX block
        <LinearLayout />

    if(true)  /* Block Comment between `if` and KTX block */
        <LinearLayout />
}
