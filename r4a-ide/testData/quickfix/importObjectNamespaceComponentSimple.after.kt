// "Import" "true"
// ERROR: Unresolved reference: MyNamespace

package testing

import com.google.r4a.*
import foobar.MyNamespace

fun testing() {
  <MyNamespace.MyComponent />
}
