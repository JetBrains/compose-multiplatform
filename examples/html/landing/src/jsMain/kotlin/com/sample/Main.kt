package com.sample

import androidx.compose.runtime.NoLiveLiterals
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.renderComposable
import com.sample.components.*
import com.sample.content.*
import com.sample.style.AppStylesheet
import kotlinx.browser.document
import kotlinx.browser.window

@NoLiveLiterals
fun main() {
    val scene = THREE.Scene()
    val camera = THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000 )
    val renderer = THREE.WebGLRenderer()
    renderer.setSize( window.innerWidth, window.innerHeight )
    document.body?.appendChild( renderer.domElement )

    val geometry = THREE.BoxGeometry(1,1,1)
    val material = THREE.MeshBasicMaterial(js("{color: 0x00ff00}"))
    val cube = THREE.Mesh(geometry, material)
    scene.add(cube)
    camera.asDynamic().position.z = 5 // TODO: add corresponding types and properties to Camera instead of dynamic

    fun animate() {
        window.requestAnimationFrame { animate() }
        // TODO: add corresponding types instead of dynamic
        cube.asDynamic().rotation.x += 0.01;
        cube.asDynamic().rotation.y += 0.01;
        renderer.render(scene, camera)
    }

    animate()
}