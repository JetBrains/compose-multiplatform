// "Import" "true"
// ERROR: Unresolved reference: MyComponent
// ERROR: Unresolved reference: MyComponent

package testing

import foobar.MyComponent

fun testing() {
  <MyComponent></MyComponent>
}
