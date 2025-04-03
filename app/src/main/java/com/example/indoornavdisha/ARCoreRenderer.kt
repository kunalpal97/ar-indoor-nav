package com.example.indoornavdisha

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.google.ar.core.Session
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARCoreRenderer(
    private val context: Context,
    private val session: Session
) : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Clear screen to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // Set the OpenGL viewport to the same size as the surface
        GLES20.glViewport(0, 0, width, height)

        // Update the session display geometry
        session.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        try {
            // Get the latest frame from ARCore
            val frame = session.update()
            // Draw the camera background
            frame.acquireCameraImage().close() // Just to access the camera, not actually using the image
        } catch (e: Exception) {
            // Handle ARCore errors
        }
    }
}