//package com.example.indoornavdisha
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.widget.Button
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import io.github.sceneview.ar.ARSceneView
//
//class MainActivity : AppCompatActivity() {
//
//    private val cameraPermissionCode = 0
//    private lateinit var arSceneView: ARSceneView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        // Find the button and set click listener
//        val openCameraButton = findViewById<Button>(R.id.open_camera_button)
//        openCameraButton.setOnClickListener {
//            if (hasCameraPermission()) {
//                openArCamera()
//            } else {
//                requestCameraPermission()
//            }
//        }
//    }
//
//    private fun hasCameraPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
//                PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun requestCameraPermission() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(Manifest.permission.CAMERA),
//            cameraPermissionCode
//        )
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == cameraPermissionCode) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                openArCamera()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Camera permission is required",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    private fun openArCamera() {
//        // Switch to AR view layout
//        setContentView(R.layout.ar_view)
//
//        // Get the AR Scene View
//        arSceneView = findViewById(R.id.arSceneView)
//
//        // ArSceneView automatically handles the AR session and camera rendering
//
//        Toast.makeText(this, "AR Camera opened!", Toast.LENGTH_SHORT).show()
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        // ArSceneView handles the session lifecycle automatically if initialized
//        if (::arSceneView.isInitialized) {
//            arSceneView.onSessionResumed
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//
//        // ArSceneView handles the session lifecycle automatically if initialized
//        if (::arSceneView.isInitialized) {
////            arSceneView.pause()
//            arSceneView.onSessionPaused
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//
//        // ArSceneView handles cleanup automatically if initialized
//        if (::arSceneView.isInitialized) {
//            arSceneView.destroy()
//        }
//    }
//}


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
import com.google.android.filament.gltfio.AssetLoader
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.loaders.ModelLoader

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity" // Tag for logging
    private val cameraPermissionCode = 0 // Request code for camera permission
    private lateinit var arSceneView: ARSceneView // The AR view
    private val placedMarkers = mutableListOf<AnchorNode>() // List to track all placed markers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the initial layout with the "Open Camera" button
        setContentView(R.layout.activity_main)

        // Find the button and set a click listener
        val openCameraButton = findViewById<Button>(R.id.open_camera_button)
        openCameraButton.setOnClickListener {
            // Check for camera permission before opening AR view
            if (hasCameraPermission()) {
                openArCamera()
            } else {
                requestCameraPermission()
            }
        }
    }

    // Check if we already have camera permission
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    // Request camera permission if we don't have it
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            cameraPermissionCode
        )
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // If permission granted, open the AR camera
                openArCamera()
            } else {
                // If permission denied, show a message
                Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Set up and open the AR camera view
    private fun openArCamera() {
        // Switch to AR view layout
        setContentView(R.layout.ar_view)

        // Get the AR Scene View from the layout
        arSceneView = findViewById(R.id.arSceneView)

        // Enable plane detection so user can see detected surfaces
        arSceneView.planeRenderer.isEnabled = true

        // Set up touch handling for AR interactions
        setupTouchInteraction()

        Toast.makeText(this, "AR Camera opened! Tap on a surface to place a marker", Toast.LENGTH_SHORT).show()
    }

    // Set up touch interaction for placing markers
    private fun setupTouchInteraction() {
        arSceneView.setOnTouchListener { view, event ->
            // Only respond to tap down events (when finger first touches the screen)
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Perform hit test to see what's under the user's finger
                performHitTest(event.x, event.y)
            }
            // Call performClick for accessibility
            view.performClick()
            true
        }
    }

    // Perform a hit test to detect what's under the user's finger
    private fun performHitTest(x: Float, y: Float) {
        // Get the current AR frame from SceneView
        val arFrame = arSceneView.frame

        if (arFrame != null) {
            // Perform a hit test at the tap location
            val hitResults = arFrame.hitTest(x, y)

            // Process hit results
            processHitResults(hitResults)
        }
    }

    // Process the hit test results to find suitable surfaces
    private fun processHitResults(hitResults: List<HitResult>) {
        // Find the first hit result that is on a plane
        val planeHitResult = hitResults.firstOrNull { hit ->
            val trackable = hit.trackable
            // Only select hits that are on detected planes
            trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
        }

        // If we found a valid hit on a plane, place a marker there
        if (planeHitResult != null) {
            placeMarkerAtHit(planeHitResult)
        } else {
            // If no plane was hit, inform the user
            Toast.makeText(this, "Please tap on a detected surface", Toast.LENGTH_SHORT).show()
        }
    }

    // Place a marker at the hit location
    // Place a marker at the hit location
    private fun placeMarkerAtHit(hitResult: HitResult) {
        try {
            // Create an anchor at the hit location - this keeps the marker fixed in real world space
            val anchor = hitResult.createAnchor()

            // Get the engine from ARSceneView
            val engine = arSceneView.engine

            // Create a new anchor node with the engine
            val anchorNode = AnchorNode(engine, anchor)

            // Set the anchor separately
            anchorNode.anchor = anchor

            // Add the anchor node to the scene
            arSceneView.addChildNode(anchorNode)

            // Create a visible marker as a child of the anchor
            createVisibleMarker(anchorNode)

            // Store the marker reference so we can manage it later if needed
            placedMarkers.add(anchorNode)

            // Provide feedback to the user
            Toast.makeText(this, "Marker placed!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Log and display any errors
            Log.e(tag, "Error placing marker: ${e.message}")
            Toast.makeText(this, "Failed to place marker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createVisibleMarker(parentNode: AnchorNode) {
        try {
            // Step 1: Get the engine from the AR SceneView.
            val engine = arSceneView.engine

            // Step 2: Create a marker node to serve as a fixed container.
            val markerNode = io.github.sceneview.node.Node(engine)
            parentNode.addChildNode(markerNode)

            // Step 3: Initialize the model loader.
            val modelLoader = ModelLoader(engine = engine, context = this)

            // Step 4: Asynchronously load the 3D model.
            modelLoader.loadModelAsync(
                fileLocation = "test.glb",
                onResult = { loadedModel ->
                    if (loadedModel != null) {
                        // Step 5: Create a model node using the loaded model.
                        val modelInstance = modelLoader.createInstance(loadedModel)
                        if (modelInstance == null) {
                            Log.e(tag, "Failed to create model instance")
                            Toast.makeText(this, "Failed to create model instance", Toast.LENGTH_SHORT).show()
                            return@loadModelAsync
                        }
                        val modelNode = io.github.sceneview.node.ModelNode(
                            modelInstance = modelInstance,  // use the loaded model here
                            autoAnimate = true
                        ).apply {
                            // Set the scale to ensure proper sizing.
                            scale = Scale(0.1f)
                            // Position it slightly above the marker's origin for better visibility.
                            position = Position(0f, 0.05f, 0f)
                        }

                        // Step 6: Add the model node as a child of the marker node.
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



    // Handle the activity lifecycle - resume AR session
    override fun onResume() {
        super.onResume()
        if (::arSceneView.isInitialized) {
            arSceneView.onSessionResumed
        }
    }

    // Handle the activity lifecycle - pause AR session
    override fun onPause() {
        super.onPause()
        if (::arSceneView.isInitialized) {
            arSceneView.onSessionPaused
        }
    }

    // Handle the activity lifecycle - clean up AR resources
    override fun onDestroy() {
        super.onDestroy()
        if (::arSceneView.isInitialized) {
            arSceneView.destroy()
        }
    }
}


//package com.example.indoornavdisha
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.util.Log
//import android.view.MotionEvent
//import android.widget.Button
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.google.ar.core.HitResult
//import com.google.ar.core.Plane
//import io.github.sceneview.ar.ARSceneView
//import io.github.sceneview.ar.localScale
//import io.github.sceneview.ar.node.AnchorNode
//import io.github.sceneview.math.Position
//import io.github.sceneview.math.Scale
//
//class MainActivity : AppCompatActivity() {
//
//    private val tag = "MainActivity" // Tag for logging
//    private val cameraPermissionCode = 0 // Request code for camera permission
//    private lateinit var arSceneView: ARSceneView // The AR view
//    private val placedMarkers = mutableListOf<AnchorNode>() // List to track all placed markers
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // Set the initial layout with the "Open Camera" button
//        setContentView(R.layout.activity_main)
//
//        // Find the button and set a click listener
//        val openCameraButton = findViewById<Button>(R.id.open_camera_button)
//        openCameraButton.setOnClickListener {
//            // Check for camera permission before opening AR view
//            if (hasCameraPermission()) {
//                openArCamera()
//            } else {
//                requestCameraPermission()
//            }
//        }
//    }
//
//    // Check if we already have camera permission
//    private fun hasCameraPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
//                PackageManager.PERMISSION_GRANTED
//    }
//
//    // Request camera permission if we don't have it
//    private fun requestCameraPermission() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(Manifest.permission.CAMERA),
//            cameraPermissionCode
//        )
//    }
//
//    // Handle the result of the permission request
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == cameraPermissionCode) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // If permission granted, open the AR camera
//                openArCamera()
//            } else {
//                // If permission denied, show a message
//                Toast.makeText(
//                    this,
//                    "Camera permission is required",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    // Set up and open the AR camera view
//    private fun openArCamera() {
//        // Switch to AR view layout
//        setContentView(R.layout.ar_view)
//
//        // Get the AR Scene View from the layout
//        arSceneView = findViewById(R.id.arSceneView)
//
//        // Enable plane detection so user can see detected surfaces
//        arSceneView.planeRenderer.isEnabled = true
//
//        // Set up touch handling for AR interactions
//        setupTouchInteraction()
//
//        Toast.makeText(this, "AR Camera opened! Tap on a surface to place a marker", Toast.LENGTH_SHORT).show()
//    }
//
//    // Set up touch interaction for placing markers
//    private fun setupTouchInteraction() {
//        arSceneView.setOnTouchListener { view, event ->
//            // Only respond to tap down events (when finger first touches the screen)
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                // Perform hit test to see what's under the user's finger
//                performHitTest(event.x, event.y)
//            }
//            // Call performClick for accessibility
//            view.performClick()
//            true
//        }
//    }
//
//    // Perform a hit test to detect what's under the user's finger
//    private fun performHitTest(x: Float, y: Float) {
//        // Get the current AR frame from SceneView
//        val arFrame = arSceneView.frame
//
//        if (arFrame != null) {
//            // Perform a hit test at the tap location
//            val hitResults = arFrame.hitTest(x, y)
//
//            // Process hit results
//            processHitResults(hitResults)
//        }
//    }
//
//    // Process the hit test results to find suitable surfaces
//    private fun processHitResults(hitResults: List<HitResult>) {
//        // Find the first hit result that is on a plane
//        val planeHitResult = hitResults.firstOrNull { hit ->
//            val trackable = hit.trackable
//            // Only select hits that are on detected planes
//            trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
//        }
//
//        // If we found a valid hit on a plane, place a marker there
//        if (planeHitResult != null) {
//            placeMarkerAtHit(planeHitResult)
//        } else {
//            // If no plane was hit, inform the user
//            Toast.makeText(this, "Please tap on a detected surface", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // Place a marker at the hit location
//    // Place a marker at the hit location
//    private fun placeMarkerAtHit(hitResult: HitResult) {
//        try {
//            // Create an anchor at the hit location - this keeps the marker fixed in real world space
//            val anchor = hitResult.createAnchor()
//
//            // Get the engine from ARSceneView
//            val engine = arSceneView.engine
//
//            // Create a new anchor node with the engine
//            val anchorNode = AnchorNode(engine, anchor)
//
//            // Set the anchor separately
//            anchorNode.anchor = anchor
//
//            // Add the anchor node to the scene
//            arSceneView.addChildNode(anchorNode)
//
//            // Create a visible marker as a child of the anchor
//            createVisibleMarker(anchorNode)
//
//            // Store the marker reference so we can manage it later if needed
//            placedMarkers.add(anchorNode)
//
//            // Provide feedback to the user
//            Toast.makeText(this, "Marker placed!", Toast.LENGTH_SHORT).show()
//        } catch (e: Exception) {
//            // Log and display any errors
//            Log.e(tag, "Error placing marker: ${e.message}")
//            Toast.makeText(this, "Failed to place marker: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // Create a visible 3D object to represent the marker
//    private fun createVisibleMarker(parentNode: AnchorNode) {
//        // Create a new node as a child of the anchor node
//        val engine = arSceneView.engine
//        val markerNode = io.github.sceneview.node.Node(engine)
//        parentNode.addChildNode(markerNode)
//
//        // Set the scale to make it visible but not too large
//        markerNode.scale = Scale(0.1f)
//
//        // Position it slightly above the surface to ensure visibility
//        markerNode.position = Position(0f, 0.05f, 0f)
//
//        io.github.sceneview.model.Model = Model.build()
//            .setSource(this, "models/pin.glb")
//            .setAutoAnimate(true)
//            .build()
//            .thenAccept { model ->
//                markerNode.model = model
//                Log.d(tag, "3D model loaded successfully")
//            }
//            .exceptionally { throwable ->
//                Log.e(tag, "Error loading model: ${throwable.message}")
//                Toast.makeText(this, "Could not load marker model", Toast.LENGTH_SHORT).show()
//                null
//            }
//    } catch (e: Exception) {
//        Log.e(tag, "Error in createVisibleMarker: ${e.message}")
//    }
//}
//
//    // Handle the activity lifecycle - resume AR session
//    override fun onResume() {
//        super.onResume()
//        if (::arSceneView.isInitialized) {
//            arSceneView.onSessionResumed
//        }
//    }
//
//    // Handle the activity lifecycle - pause AR session
//    override fun onPause() {
//        super.onPause()
//        if (::arSceneView.isInitialized) {
//            arSceneView.onSessionPaused
//        }
//    }
//
//    // Handle the activity lifecycle - clean up AR resources
//    override fun onDestroy() {
//        super.onDestroy()
//        if (::arSceneView.isInitialized) {
//            arSceneView.destroy()
//        }
//    }
//}