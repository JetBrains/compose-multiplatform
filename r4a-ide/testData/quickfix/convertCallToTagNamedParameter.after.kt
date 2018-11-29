// "Convert to a tag" "true"
// ERROR: Stateless Functional Components (SFCs) should not be invoked, use <Test /> syntax instead

package testing

import com.google.r4a.*

@Composable
fun Test(p1: String, p2: String) { }

@Composable
fun App(v1: String, v2: String) {
    <Test p2=v2 p1=v1 />
}