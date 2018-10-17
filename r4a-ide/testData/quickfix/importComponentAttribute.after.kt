// "Import" "true"
// ERROR: No valid attribute on 'class MyComponent : Component' found with key 'foo' and type 'Int'

package testing

import foobar.MyComponent
import foobar.setFoo

fun testing() {
  <MyComponent foo=123 />
}
