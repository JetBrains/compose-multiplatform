// "Import" "true"
// ERROR: Invalid KTX tag type. Expected 'Emittable, View, ViewGroup'
// ERROR: Unresolved reference: MyComponent

package testing

import com.google.r4a.*

fun testing() {
  <MyComponent<caret>></MyComponent>
}
