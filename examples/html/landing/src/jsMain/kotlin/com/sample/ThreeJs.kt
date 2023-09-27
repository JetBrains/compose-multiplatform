package com.sample

import org.w3c.dom.HTMLElement

//@JsName("THREE")
@JsModule("three")
@JsNonModule
external object THREE {

    class Scene {
        fun add(a: dynamic)
    }

    class PerspectiveCamera(a: Number, b: Number, c: Number, d: Number)

    class WebGLRenderer {

        val domElement: HTMLElement
        fun setSize(w: Number, h: Number)

        fun render(scene: Scene, camera: PerspectiveCamera)
    }

    class BoxGeometry(a: Number, b: Number, c: Number)

    class MeshBasicMaterial(props: dynamic)

    class Mesh(geometry: BoxGeometry, material: MeshBasicMaterial)

}