// "Convert to a tag" "true"
// ERROR: Stateless Functional Components (SFCs) should not be invoked, use <Test /> syntax instead

package testing

import com.google.r4a.*

@Composable
fun Test(text: String) { }

@Composable
fun App(value: String) {
    Test<caret>(value)
}