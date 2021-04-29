# Building the UI with Compose Web

**The API is experimental and breaking changes can be expected**

## What is covered

In this tutorial we will look at several examples with Composable DOM DSL code.

### Entry point

Compose Web manages the DOM tree or a subtree. It needs an HTML node that will be a root of the composition.

```kotlin
renderComposable(rootElementId = "root") {
    // content goes here
}
```

### HTML tags in Compose DOM DSL

Compose DOM DSL doesn't have a Composable for every HTML tag yet.  
Mostly used tags can be used out of a box.

Let's have a look at `Div` (most other tags have the same signature):

```kotlin
Div(
    attrs = {
        // specify attributes here
    },
    style = {
        // specify inline style here
    }
) {
    // div content goes here
}
```

Let's have a look at `Input`, its signature is a bit different:

```kotlin
Input(
    type = InputType.Text, // All InputTypes supported
    value = "", // sets the input value
    attrs = {},
    style = {}
)
```

Input type can also be specified in `attrs` block:

```kotlin
Input(attrs = { type(InputType.Text) })
```

To make it more convenient, `Input` and some other tags like `A`, `Form`, `Img` have extra parameters in the signature,
so `attrs` block could be skipped.

### Text

`Text` allows to add a text content. It doesn't have any parameters:

```kotlin
Text("Arbitrary text")
```

To style the text it needs to be wrapped in a container with a style applied:

```kotlin
Span(
    style = { color("red") } // inline style
) {
    Text("Red text")
}
```

In HTML, it will look like this:
```html
<span style="color: red;">Red text</span>
```

### Attributes

In the examples above we've already seen `attrs` parameter.
It allows us to specify element's attributes and properties.

Currently, not all attributes are supported out of a box.
Therefore, let's start with setting a custom (or not yet supported) attribute:

```kotlin
Div(
    attrs = {
        attr(attr = "custom_attr", value = "its_value")
    }
) { /* content */ }
```

#### Common attributes

Let's have a look at some common attributes that can be used out of a box:

```kotlin
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
Some elements have specific attributes (meaningful only to that particular tag).

For example, in tag `A` there are available all common attributes + `A` specific attributes:

```kotlin
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

There are also specific attributes for the following elements:
- Button
- Form
- Input
- Option
- Select
- OptGroup
- TextArea
- Img

We plan to add detailed documentation for them later on.

#### Events

To handle an event the event listener has to be declared in `attrs` block:

```kotlin
Button(
    attrs = { 
        onClick { println("Button clicked") }
    }
) { Text("Button") }
```

There are more examples about events handling here - [Events Handling](../Events_Handling/README.md)

### Style

There are two options to set components' style:
- Using inline style
- Using Stylesheet

Now we quickly have a look at `style` block where inline style can be defined:

```kotlin

Div(
    style = {
        display(DisplayStyle.Flex)
        padding(20.px)
        
        // custom property
        property("font-family", value("Arial, Helvetica, sans-serif"))
    }
) { /* content goes here */ }
```

More detailed overview of Style DSL and examples can be found here - [Style DSL](../Style_Dsl/README.md)