package com.example.indoornavdisha

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.Pose
import com.google.ar.core.Anchor
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.loaders.ModelLoader

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"
    private val cameraPermissionCode = 0
    private lateinit var arSceneView: ARSceneView
    private val placedMarkers = mutableListOf<AnchorNode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the initial layout with the "Open Camera" button
        setContentView(R.layout.activity_main)

        val openCameraButton = findViewById<Button>(R.id.open_camera_button)
        openCameraButton.setOnClickListener {
            if (hasCameraPermission()) {
                openArCamera()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            cameraPermissionCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openArCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openArCamera() {
        // Switch to AR view layout
        setContentView(R.layout.ar_view)
        arSceneView = findViewById(R.id.arSceneView)
        // Enable plane rendering if you still want to see detected planes (optional)
        arSceneView.planeRenderer.isEnabled = true

        // Set up touch handling for direct marker placement
        setupTouchInteraction()

        Toast.makeText(
            this,
            "AR Camera opened! Tap anywhere to place a marker",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Setup touch listener to place a marker directly on touch.
    private fun setupTouchInteraction() {
        arSceneView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                placeMarkerDirectly()
            }
            view.performClick()
            true
        }
    }

    // Directly create an anchor relative to the current camera pose with a fixed offset.
    private fun placeMarkerDirectly() {
        val arFrame = arSceneView.frame
        if (arFrame != null && arFrame.camera.trackingState == TrackingState.TRACKING) {
            val cameraPose = arFrame.camera.pose

            // How far in front of the camera to place the marker
            val forwardOffset = 1.0f

            // Get a forward offset based on camera's z-axis
            val dx = -cameraPose.zAxis[0] * forwardOffset
            val dz = -cameraPose.zAxis[2] * forwardOffset

            // Fixed Y-level for the floor (assuming flat ground)
            val floorY = 0f

            // Construct a new pose in front of the camera, on the floor
            val newPose = Pose.makeTranslation(
                cameraPose.tx() + dx,
                floorY,
                cameraPose.tz() + dz
            )

            // Create an anchor and place marker if session is available
            arSceneView.session?.let { session ->
                val anchor: Anchor = session.createAnchor(newPose)
                placeMarkerAtAnchor(anchor)
            } ?: run {
                Toast.makeText(this, "AR session is not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "AR Frame not available or tracking lost", Toast.LENGTH_SHORT).show()
        }
    }


    // Place a marker using the given anchor.
    private fun placeMarkerAtAnchor(anchor: Anchor) {
        try {
            val engine = arSceneView.engine
            val anchorNode = AnchorNode(engine, anchor)
            anchorNode.anchor = anchor
            arSceneView.addChildNode(anchorNode)
            createVisibleMarker(anchorNode)
            placedMarkers.add(anchorNode)
            Toast.makeText(this, "Marker placed!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(tag, "Error placing marker: ${e.message}")
            Toast.makeText(this, "Failed to place marker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Create a visible marker model as a child of the given anchor node.
    private fun createVisibleMarker(parentNode: AnchorNode) {
        try {
            val engine = arSceneView.engine
            val markerNode = io.github.sceneview.node.Node(engine)
            parentNode.addChildNode(markerNode)

            val modelLoader = ModelLoader(engine = engine, context = this)
            modelLoader.loadModelAsync(
                fileLocation = "test.glb",
                onResult = { loadedModel ->
                    if (loadedModel != null) {
                        val modelInstance = modelLoader.createInstance(loadedModel)
                        if (modelInstance == null) {
                            Log.e(tag, "Failed to create model instance")
                            Toast.makeText(this, "Failed to create model instance", Toast.LENGTH_SHORT).show()
                            return@loadModelAsync
                        }
                        val modelNode = io.github.sceneview.node.ModelNode(
                            modelInstance = modelInstance,
                            autoAnimate = true
                        ).apply {
                            scale = Scale(0.1f)
                            position = Position(0f, 0.05f, 0f)
                        }
                        markerNode.addChildNode(modelNode)
                        Log.d(tag, "3D model loaded successfully")
                    } else {
                        Log.e(tag, "Failed to load 3D model")
                        Toast.makeText(this, "Could not load marker model", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Error in createVisibleMarker: ${e.message}")
            Toast.makeText(this, "Failed to create marker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::arSceneView.isInitialized) {
            arSceneView.onSessionResumed
        }
    }

    override fun onPause() {
        super.onPause()
        if (::arSceneView.isInitialized) {
            arSceneView.onSessionPaused
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::arSceneView.isInitialized) {
            arSceneView.destroy()
        }
    }
}