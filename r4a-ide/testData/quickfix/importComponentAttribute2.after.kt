// "Import" "true"
// ERROR: Unresolved reference: foo
// ERROR: Unresolved reference: unresolved

package testing

import foobar.MyComponent
import foobar.setFoo

fun testing() {
  <MyComponent foo=unresolved />
}
