// "Import" "true"
// ERROR: Unresolved reference: MyNamespace

package testing

import com.google.r4a.*

fun testing() {
  <MyNamespace<caret>.MyComponent />
}
