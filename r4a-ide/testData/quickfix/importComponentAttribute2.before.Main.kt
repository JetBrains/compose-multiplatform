// "Import" "true"
// ERROR: No valid attribute on 'constructor MyComponent(), class MyComponent : Component, fun invoke(): Unit' found with key 'foo'
// ERROR: Unresolved reference: unresolved

package testing

import com.google.r4a.*
import foobar.MyComponent

fun testing() {
  <MyComponent <caret>foo=unresolved />
}
