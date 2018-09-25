
fun f(){
    <com.google.<caret>
}

// EXIST: { lookupString: "r4a", itemText: "<com.google.r4a.* />" }
// NOTHING_ELSE
