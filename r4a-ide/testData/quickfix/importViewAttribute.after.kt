// "Import" "true"
// ERROR: No valid attribute on 'constructor TextView(context: Context!), class TextView : View, ViewTreeObserver.OnPreDrawListener' found with key 'foo' and type 'Int'

package testing

import android.widget.TextView
import com.google.r4a.*
import foobar.setFoo

fun testing() {
  <TextView foo=123 />
}
