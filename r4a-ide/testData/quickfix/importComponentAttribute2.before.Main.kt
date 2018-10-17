// "Import" "true"
// ERROR: Unresolved reference: foo
// ERROR: Unresolved reference: unresolved

package testing

import foobar.MyComponent

fun testing() {
  <MyComponent <caret>foo=unresolved />
}
