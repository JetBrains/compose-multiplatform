// "Import" "true"
// ERROR: Invalid KTX tag type. Expected 'Emittable, View, ViewGroup'
// ERROR: Unresolved reference: MyLinearLayout

package testing

import com.google.r4a.*

fun testing() {
  <MyLinearLayout<caret> />
}
