package com.example.indoornavdisha

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import com.example.indoornavdisha.WaypointStorage

// Data model for a waypoint response
data class Waypoint(
    val waypoint_id: Int,
    val x: Float,
    val y: Float,
    val z: Float
)

// Data model for the API response
data class WaypointsResponse(
    val message: String,
    val waypoints: List<Waypoint>
)

// Retrofit interface for API communication
interface ApiService {
    @Multipart
    @POST("upload")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<WaypointsResponse>

    companion object {
        fun create(): ApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl("http://192.168.1.215:5000/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

class SecondActivity : AppCompatActivity() {

    private val TAG = "SecondActivity"

    private lateinit var uploadButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var resultText: TextView

    private var selectedImageUri: Uri? = null

    private val apiService by lazy { ApiService.create() }

    // Register activity result for selecting an image
    private val getImageContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    imagePreview.setImageURI(uri)
                    Toast.makeText(this, "Image selected. Ready to upload.", Toast.LENGTH_SHORT).show()
                    uploadButton.text = "Upload Selected Image"
                    uploadButton.setOnClickListener { uploadSelectedImage() }
                }
            }
        }

    // Register permission request for storage access
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_view)

        uploadButton = findViewById(R.id.uploadButton)
        imagePreview = findViewById(R.id.imagePreview)
        resultText = findViewById(R.id.resultText)

        uploadButton.setOnClickListener { checkPermissionAndPickImage() }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageContent.launch(intent)
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "image_upload.jpg"
    }

    private fun uploadSelectedImage() {
        val imageUri = selectedImageUri
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        uploadButton.isEnabled = false
        uploadButton.text = "Uploading..."
        resultText.text = "Processing image..."

        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val parcelFileDescriptor = contentResolver.openFileDescriptor(imageUri, "r")
                    val inputStream = FileInputStream(parcelFileDescriptor?.fileDescriptor)
                    val originalFileName = getFileName(imageUri)
                    val tempFile = File(cacheDir, originalFileName)
                    val outputStream = FileOutputStream(tempFile)
                    inputStream.copyTo(outputStream)
                    outputStream.close()
                    tempFile
                }

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

                val response = apiService.uploadImage(body)
                if (response.isSuccessful) {
                    val waypointsResponse = response.body()

                    // Store waypoints in the global object
                    if (waypointsResponse != null) {
                        WaypointStorage.waypoints = waypointsResponse.waypoints
                    }

                    // Optionally format and display waypoints here
                    val formattedWaypoints = if (WaypointStorage.waypoints.isNotEmpty()) {
                        WaypointStorage.waypoints.joinToString("\n") { waypoint ->
                            "Waypoint ${waypoint.waypoint_id}: (${waypoint.x}, ${waypoint.y}, ${waypoint.z})"
                        }
                    } else {
                        "No waypoints returned."
                    }
                    resultText.text = "Upload successful!\n\nWaypoints:\n$formattedWaypoints"

                    // Now call MainActivity after storing waypoints
                    val intent = Intent(this@SecondActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Close SecondActivity if desired

                } else {
                    resultText.text = "Error: ${response.code()} - ${response.message()}"
                    uploadButton.text = "Retry Upload"
                    uploadButton.isEnabled = true
                }
            } catch (e: Exception) {
                resultText.text = "Upload failed: ${e.message}"
                uploadButton.text = "Retry Upload"
                uploadButton.isEnabled = true
            }
        }
    }
}