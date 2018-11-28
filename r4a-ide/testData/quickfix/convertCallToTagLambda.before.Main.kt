// "Convert to a tag" "true"
// ERROR: Stateless Functional Components (SFCs) should not be invoked, use <Test /> syntax instead

package testing

import com.google.r4a.*

@Composable
fun Test(p1: String, @Children children: () -> Unit) { }

@Composable
fun Test2() {}

@Composable
fun App(v1: String, v2: String) {
    Test<caret>(p1 = v1) {
        <Test2 />
    }
}