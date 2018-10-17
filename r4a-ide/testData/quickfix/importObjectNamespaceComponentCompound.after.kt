// "Import" "true"
// ERROR: Unresolved reference: MyNamespace
// ERROR: Unresolved reference: MyNamespace
// ERROR: Element has a children body provided, but no @Children declarations were found

package testing

import foobar.MyNamespace

fun testing() {
  <MyNamespace.MyComponent></MyNamespace.MyComponent>
}
