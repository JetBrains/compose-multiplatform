// "Import" "true"
// ERROR: Unresolved reference: MyNamespace
// ERROR: Unresolved reference: MyNamespace
// ERROR: Element has a children body provided, but no @Children declarations were found

package testing

fun testing() {
  <MyNamespace<caret>.MyComponent></MyNamespace.MyComponent>
}
