import androidx.compose.runtime.Composable

class Abc


val Abc.commonIntVal: Int
    @Composable get() = 1000

expect val Abc.composableIntVal: Int
    @Composable get

@Composable
fun GetIntVal(): Int {
    return Abc().composableIntVal
}

@Composable
fun GetIntValWithDefault(def: Int = Abc().composableIntVal): Int {
    return def
}