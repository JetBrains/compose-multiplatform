# Building the UI with the Compose HTML library

In this tutorial we will look at several examples that use the Composable HTML/CSS DSL to describe the user interface for your web application.

### Entry point

The Compose HTML library needs an HTML node that will be a root of its composition. Inside this root node, Compose then manages its own DOM tree.

``` kotlin
renderComposable(rootElementId = "root") {
    // content goes here
}
```

### HTML tags

Let's have a look at the Composable for a `Div` tag (most other tags have the same signature):

``` kotlin
Div(
    attrs = {
        // specify attributes here
        style {
            // specify inline style here
        }
    }
) {
    // div content goes here
}
```

For convenience, some tags like `Input`, `A`, `Form`, or `Img` allow you to specify some extra parameters in the signature that are specific to the respective HTML tag. For example, let’s look at the `Input` tag:

``` kotlin
Input(
    type = InputType.Text, // All InputTypes supported
    attrs = {}
)
```

We can use the `type` parameter which is provided for our convenience, or can use the `attrs` block to specify the input type:

``` kotlin
Input(attrs = { type(InputType.Text) })
```

### Text

The `Text` allows you to add text content to an HTML tag. Besides the text content it represents, it does not have any parameters:

``` kotlin
Text("Arbitrary text")
```

If you want to apply styles to text, it needs to be wrapped in a container with a style applied, like a `Span` or `P`:

``` kotlin
Span(
    attrs = { style { color(Color.red) } } // inline style
) {
    Text("Red text")
}
```

This corresponds to the following HTML code:
```html
<span style="color: red;">Red text</span>
```

### Attributes

The `attrs` parameter (which we’ve already seen in some of the previous examples) allows us to specify element's attributes and properties.

The most flexible way to define attributes is by using the `attr` function, which allows you to specify the attribute name and its value.

``` kotlin
Div(
    attrs = {
        attr(attr = "custom_attr", value = "its_value")
    }
) { /* content */ }
```

However, with this approach, Compose is not able to validate that the attribute exists on the HTML element, or is valid. This is why we also provide a set of helper functions for common attributes.

#### Common attributes

Here are some examples of common attributes that are available for most Composables representing HTML tags:

``` kotlin
attrs = {
    id("elementId")
    classes("cl1", "cl2")
    hidden(false)
    title("title")
    draggable(Draggable.Auto)
    dir(DirType.Auto)
    lang("en")
    contentEditable(true)
}
```

#### Element specific attributes 

Depending on the element you are working with, you may also have access to some specific attributes – attributes that are only meaningful for this particular tag. For example, the `A` tag provides some specific attributes, that are specific to hyperlinks:

``` kotlin
A(
    attrs = {
        href("https://localhost:8080/page2")
        target(ATarget.Blank)
        rel(ARel.Next)
        hreflang("en")
        download("https://...")
    }
) {}
```

Some other elements that provide specific attributes include:
- Button
- Form
- Input
- Option
- Select
- OptGroup
- TextArea
- Img

To discover all attributes that are available in your current scope, you can use your IDE’s autocomplete feature. As we evolve these APIs, we also plan to add detailed documentation for them.

#### Events

You can declare event listeners in the `attrs` block:

``` kotlin
Button(
    attrs = { 
        onClick { println("Button clicked") }
    }
) { Text("Button") }
```

There are more examples about events handling here - [Events Handling](../Events_Handling/README.md)

### Style

There are ways to set the style for a component:
- Using inline styles
- Using stylesheets

You can declare inline styles via the `style` block of a component:

``` kotlin
Div(
    attrs = {
        style {
            display(DisplayStyle.Flex)
            padding(20.px)
            
            // custom property
            property("font-family", "Arial, Helvetica, sans-serif")
        }
    }
) { /* content goes here */ }
```

You can find a more detailed overview of the style DSL, as well as additional examples here - [Style DSL](../Style_Dsl/README.md)

### Runnable example

```kotlin
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        Div(
            attrs = {
                // specify attributes here
                style {
                    // specify inline style here
                }
            }
        ) {
            Text("A text in <div>")
        }

        Input(
            type = InputType.Text, // All InputTypes supported
            attrs = {}
        )

        Text("Arbitrary text")

        Span({
            style { color(Color.red) } // inline style
        }) {
            Text("Red text")
        }

        Div(
            attrs = {
                id("elementId")
                classes("cl1", "cl2")
                hidden()
                title("title")
                draggable(Draggable.Auto)
                dir(DirType.Auto)
                lang("en")
                contentEditable(true)

                // custom attr
                attr(attr = "custom_attr", value = "its_value")
            }
        ) { /* content */ }

        A(
            attrs = {
                href("https://localhost:8080/page2")
                target(ATarget.Blank)
                hreflang("en")
                download("https://...")
            }
        ) { Text("Link") }

        Button(
            attrs = {
                onClick { println("Button clicked") }
            }
        ) { Text("Button") }

        Div({
            style {
                display(DisplayStyle.Flex)
                padding(20.px)

                // custom property
                property("font-family", "Arial, Helvetica, sans-serif")
            }
        }) { Text("Text in Div with inline style") }
    }
}
```
