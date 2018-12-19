// "Import" "true"
// ERROR: Invalid KTX tag type. Expected 'Emittable, View, ViewGroup'
// ERROR: Unresolved reference: LinearLayout

package testing

import com.google.r4a.*

fun testing() {
  <LinearLayout<caret> />
}
