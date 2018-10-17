// "Import" "true"
// ERROR: Unresolved reference: MyNamespace

package testing

import foobar.MyNamespace

fun testing() {
  <MyNamespace.MyComponent />
}
