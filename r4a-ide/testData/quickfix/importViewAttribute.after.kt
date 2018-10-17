// "Import" "true"
// ERROR: No valid attribute on 'class TextView : View, ViewTreeObserver.OnPreDrawListener' found with key 'foo' and type 'Int'

package testing

import android.widget.TextView
import foobar.setFoo

fun testing() {
  <TextView foo=123 />
}
