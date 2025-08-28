package mapbox.navigation.sdk.mapbox_navigation_sdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class NavigationActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var navigationStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        setupNavigation()
    }

    override fun onBackPressed() {
        returnToFlutterWithRoute()
    }

    override fun finish() {
        returnToFlutterWithRoute()
    }
    
    private fun returnToFlutterWithRoute() {
        // Send navigation cancelled event to Flutter via plugin
        MapboxNavigationSdkPlugin.pluginInstance?.sendNavigationEvent(
            "NavigationEventType.navigationCancelled", 
            emptyMap<String, Any>()
        )
        super.finish()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupNavigation()
            } else {
                showError("Location permission is required for navigation")
            }
        }
    }

    private fun setupNavigation() {
        val destinationLat = intent.getDoubleExtra("destinationLatitude", 0.0)
        val destinationLng = intent.getDoubleExtra("destinationLongitude", 0.0)
        
        try {
            // For now, show a simple mock navigation screen
            // In a real implementation, you would integrate with Mapbox Navigation SDK
            showMockNavigation(destinationLat, destinationLng)
            
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Navigation SDK Error: ${e.message}")
        }
    }

    private fun showMockNavigation(destinationLat: Double, destinationLng: Double) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF4CAF50.toInt())
        }

        val titleView = TextView(this).apply {
            text = "🗺️ Mock Navigation Active"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleView)

        val destinationView = TextView(this).apply {
            text = "Destination: $destinationLat, $destinationLng"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, 32)
        }
        layout.addView(destinationView)

        val finishButton = Button(this).apply {
            text = "Finish Navigation"
            setOnClickListener { 
                // Send arrival event and finish
                MapboxNavigationSdkPlugin.pluginInstance?.sendNavigationEvent(
                    "NavigationEventType.navigationArrive", 
                    emptyMap<String, Any>()
                )
                finish()
            }
        }
        layout.addView(finishButton)

        val cancelButton = Button(this).apply {
            text = "Cancel Navigation"
            setOnClickListener { 
                returnToFlutterWithRoute()
            }
        }
        layout.addView(cancelButton)

        setContentView(layout)

        // Send navigation running event
        MapboxNavigationSdkPlugin.pluginInstance?.sendNavigationEvent(
            "NavigationEventType.navigationRunning", 
            emptyMap<String, Any>()
        )

        // Simulate some progress updates
        simulateProgress()
    }

    private fun simulateProgress() {
        var distance = 1000.0 // Start with 1km
        var duration = 300.0 // 5 minutes
        
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val progressRunnable = object : Runnable {
            override fun run() {
                if (distance > 0 && !isFinishing) {
                    val progressData = mapOf(
                        "distanceRemaining" to distance,
                        "durationRemaining" to duration
                    )
                    
                    MapboxNavigationSdkPlugin.pluginInstance?.sendNavigationEvent(
                        "NavigationEventType.progressChange", 
                        progressData
                    )
                    
                    distance -= 50.0 // Decrease by 50m each update
                    duration -= 15.0 // Decrease by 15 seconds
                    
                    handler.postDelayed(this, 2000) // Update every 2 seconds
                } else if (distance <= 0 && !isFinishing) {
                    // Arrived
                    MapboxNavigationSdkPlugin.pluginInstance?.sendNavigationEvent(
                        "NavigationEventType.navigationArrive", 
                        emptyMap<String, Any>()
                    )
                }
            }
        }
        
        handler.postDelayed(progressRunnable, 2000) // Start after 2 seconds
    }

    private fun showError(errorMessage: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFFFF5722.toInt())
        }

        val errorView = TextView(this).apply {
            text = "❌ $errorMessage"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, 32)
        }
        layout.addView(errorView)

        val backButton = Button(this).apply {
            text = "← Back to Flutter"
            setOnClickListener { finish() }
        }
        layout.addView(backButton)

        setContentView(layout)
    }
}