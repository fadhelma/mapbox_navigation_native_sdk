package mapbox.navigation.sdk.mapbox_navigation_sdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.dropin.ViewOptionsCustomization
import com.mapbox.navigation.dropin.ViewStyleCustomization
import com.mapbox.navigation.dropin.actionbutton.ActionButtonDescription

class NavigationActivity : AppCompatActivity() {

    private lateinit var navigationView: NavigationView
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var navigationStarted = false

    companion object {
        var mainActivityInstance: MainActivity? = null
    }

    // Skip route preview completely - go directly to active navigation like Image 2
    private val routesObserver = RoutesObserver { result: RoutesUpdatedResult ->
        val routes: List<NavigationRoute> = result.navigationRoutes
        println("NavigationActivity: RoutesObserver - routes count: ${routes.size}, navigationStarted: $navigationStarted")
        
        if (routes.isNotEmpty() && !navigationStarted) {
            navigationStarted = true
            println("NavigationActivity: ✅ Route ready - BYPASSING PREVIEW SCREEN!")
            println("NavigationActivity: 🚗 Going directly to active navigation (like Image 2)")
            
            // Send route built event to Flutter
            mainActivityInstance?.sendNavigationEventPublic("NavigationEventType.routeBuilt", emptyMap<String, Any>())
            
            // CRITICAL: Force transition to active navigation state immediately
            try {
                // Immediately start active guidance - this should show Image 2 state
                navigationView.api.startActiveGuidance()
                
                println("NavigationActivity: ✅ ACTIVE NAVIGATION STARTED - Should show Image 2 state!")
                
            } catch (e: Exception) {
                println("NavigationActivity: ❌ Error forcing active navigation: ${e.message}")
                showError("Failed to start active navigation: ${e.message}")
            }
        }
    }

    // Observe route progress for progress updates
    private val progressObserver = RouteProgressObserver { routeProgress ->
        val progressData = mapOf(
            "distanceRemaining" to routeProgress.distanceRemaining.toDouble(),
            "durationRemaining" to routeProgress.durationRemaining.toDouble()
        )
        mainActivityInstance?.sendNavigationEventPublic("NavigationEventType.progressChange", progressData)
    }

    // Observe trip session state for navigation running/finished events
    private val tripSessionStateObserver = TripSessionStateObserver { tripSessionState ->
        when (tripSessionState) {
            TripSessionState.STARTED -> {
                mainActivityInstance?.sendNavigationEventPublic("NavigationEventType.navigationRunning", emptyMap<String, Any>())
            }
            TripSessionState.STOPPED -> {
                mainActivityInstance?.sendNavigationEventPublic("NavigationEventType.navigationFinished", emptyMap<String, Any>())
            }
        }
    }

    // Observe arrival events
    private val arrivalObserver = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) {
            // Waypoint arrival (intermediate stops)
        }
        
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            // Next leg start
        }
        
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            // Final destination arrival
            mainActivityInstance?.sendNavigationEventPublic("NavigationEventType.navigationArrive", emptyMap<String, Any>())
        }
    }

    // Attach/detach observers to MapboxNavigation lifecycle
    private val appObserver = object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerRouteProgressObserver(progressObserver)
            mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
            mapboxNavigation.registerArrivalObserver(arrivalObserver)
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterRouteProgressObserver(progressObserver)
            mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
            mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        setupNavigation()
    }

    override fun onBackPressed() {
        // Return to Flutter with route drawn instead of completely closing
        returnToFlutterWithRoute()
    }

    override fun finish() {
        // Return to Flutter with route drawn instead of completely closing  
        returnToFlutterWithRoute()
    }
    
    private fun returnToFlutterWithRoute() {
        // Send navigation cancelled event to Flutter
        mainActivityInstance?.handleNavigationCancellation()
        // Close this activity to return to Flutter
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
        // If you need these later, they're available:
        // val arrivalRadius = intent.getDoubleExtra("arrivalRadius", 50.0)
        // val shouldSimulate = intent.getBooleanExtra("shouldSimulateRoute", false)
        // val language = intent.getStringExtra("language") ?: "en"

        try {
            val accessToken =
                "pk.eyJ1IjoiZGlnaXRhbGFzc2V0cyIsImEiOiJjbGU1Y3kzbnEwOG1kM29zNGtlcG53MWU1In0.BwMPdqNrUlDTfuQ7krvk-g"

            // Initialize once
            if (!MapboxNavigationApp.isSetup()) {
                val navOptions = NavigationOptions.Builder(this)
                    .accessToken(accessToken)
                    .build()
                MapboxNavigationApp.setup(navOptions)
            }

            // Register RoutesObserver via lifecycle
            MapboxNavigationApp.registerObserver(appObserver)


            // Layout must include a <com.mapbox.navigation.dropin.NavigationView ...> with id navigationView
            setContentView(R.layout.activity_navigation)
            navigationView = findViewById(R.id.navigationView)


            // Create destination point
            val destination = Point.fromLngLat(destinationLng, destinationLat)
            
            // IMMEDIATELY focus camera on current location when map renders
            try {
                println("NavigationActivity: IMMEDIATELY focusing camera on current location...")
                
                // Start FreeDrive mode IMMEDIATELY - this centers camera on current location
                navigationView.api.startFreeDrive()
                
                // Also try multiple immediate attempts to ensure camera focuses
                repeat(5) { attempt ->
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            navigationView.api.startFreeDrive()
                            println("NavigationActivity: ✅ FreeDrive attempt $attempt - Camera should be on current location")
                        } catch (e: Exception) {
                            // Silent fail
                        }
                    }, (attempt * 200).toLong()) // Try every 200ms
                }
                
                // STEP 2: After ensuring camera is centered, start navigation
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    println("NavigationActivity: Step 2 - Starting destination preview and navigation...")
                    
                    try {
                        // Start destination preview  
                        navigationView.api.startDestinationPreview(destination)
                        
                        // Force active guidance in multiple aggressive attempts
                        repeat(15) { attempt ->
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    navigationView.api.startActiveGuidance()
                                    println("NavigationActivity: ✅ Active guidance attempt $attempt successful!")
                                } catch (e: Exception) {
                                    // Silent fail, keep trying
                                }
                            }, (attempt * 100).toLong()) // Try every 100ms for 1.5 seconds
                        }
                        
                    } catch (e: Exception) {
                        println("NavigationActivity: Error starting navigation: ${e.message}")
                        showError("Failed to start navigation: ${e.message}")
                    }
                }, 1000) // 1 second to ensure current location is found and camera is centered
                
            } catch (e: Exception) {
                showError("Failed to focus camera on current location: ${e.message}")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            showError("Navigation SDK Error: ${e.message}\n\nDetails: ${e.cause?.message ?: "Unknown error"}")
        }
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

    override fun onDestroy() {
        MapboxNavigationApp.unregisterObserver(appObserver)
        super.onDestroy()
    }
}