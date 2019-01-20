// "Import" "true"
// ERROR: Element has a children body provided, but no @Children declarations were found
// ERROR: Unresolved reference: MyNamespace

package testing

import com.google.r4a.*

fun testing() {
  <MyNamespace<caret>.MyComponent></MyNamespace.MyComponent>
}
