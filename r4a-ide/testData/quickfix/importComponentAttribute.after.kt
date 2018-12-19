// "Import" "true"
// ERROR: No valid attribute on 'constructor MyComponent(), class MyComponent : Component, fun invoke(): Unit' found with key 'foo' and type 'Int'

package testing

import com.google.r4a.*
import foobar.MyComponent
import foobar.setFoo

fun testing() {
  <MyComponent foo=123 />
}
