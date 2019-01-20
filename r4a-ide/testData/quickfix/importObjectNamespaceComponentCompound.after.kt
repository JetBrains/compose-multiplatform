// "Import" "true"
// ERROR: Element has a children body provided, but no @Children declarations were found
// ERROR: Unresolved reference: MyNamespace

package testing

import com.google.r4a.*
import foobar.MyNamespace

fun testing() {
  <MyNamespace.MyComponent></MyNamespace.MyComponent>
}
