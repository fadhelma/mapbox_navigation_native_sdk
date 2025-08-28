package mapbox.navigation.sdk.mapbox_navigation_sdk

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "mapbox_navigation"
    private val EVENT_CHANNEL = "mapbox_navigation_events"
    private var methodChannel: MethodChannel? = null
    private var eventChannel: EventChannel? = null
    private var eventSink: EventChannel.EventSink? = null


    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "startNavigation" -> {
                    val destinationLat = call.argument<Double>("destinationLatitude")
                    val destinationLng = call.argument<Double>("destinationLongitude")
                    val arrivalRadius = call.argument<Double>("arrivalRadius") ?: 50.0
                    val shouldSimulate = call.argument<Boolean>("shouldSimulateRoute") ?: false
                    val language = call.argument<String>("language") ?: "en"
                    
                    if (destinationLat != null && destinationLng != null) {
                        startMapboxNavigation(destinationLat, destinationLng, arrivalRadius, shouldSimulate, language)
                        result.success(true)
                    } else {
                        result.error("INVALID_ARGUMENTS", "Destination coordinates required", null)
                    }
                }
                "cancelNavigation" -> {
                    // This method will be called when user confirms they want to cancel navigation
                    sendNavigationEvent("NavigationEventType.navigationCancelled", emptyMap<String, Any>())
                    result.success(null)
                    // Finish any navigation activity if it exists
                    finishAffinity()
                }
                "navigationCancelled" -> {
                    // This method will be called from NavigationActivity when user confirms cancellation
                    println("MainActivity: navigationCancelled method channel call received")
                    result.success("done closed")
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        // Setup EventChannel for navigation events
        eventChannel = EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
        eventChannel?.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })
    }

    private fun startMapboxNavigation(
        destinationLat: Double,
        destinationLng: Double,
        arrivalRadius: Double,
        shouldSimulate: Boolean,
        language: String
    ) {
        // Store reference to this MainActivity for NavigationActivity to access
        NavigationActivity.mainActivityInstance = this
        
        // Start NavigationActivity directly - no mock events
        val intent = Intent(this, NavigationActivity::class.java).apply {
            putExtra("destinationLatitude", destinationLat)
            putExtra("destinationLongitude", destinationLng)
            putExtra("arrivalRadius", arrivalRadius)
            putExtra("shouldSimulateRoute", shouldSimulate)
            putExtra("language", language)
        }
        startActivity(intent)
    }

    fun handleNavigationCancellation() {
        // This method will be called from NavigationActivity when user confirms cancellation
        println("MainActivity: handleNavigationCancellation called")
        
        // Try both event channel and method channel approaches
        sendNavigationEvent("NavigationEventType.navigationCancelled", emptyMap<String, Any>())
        
        // Also call method channel to notify Flutter directly
        try {
            methodChannel?.invokeMethod("navigationCancelled", null, object : MethodChannel.Result {
                override fun success(result: Any?) {
                    println("MainActivity: Successfully notified Flutter via method channel: $result")
                }
                override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                    println("MainActivity: Error notifying Flutter: $errorCode - $errorMessage")
                }
                override fun notImplemented() {
                    println("MainActivity: Method not implemented in Flutter")
                }
            })
        } catch (e: Exception) {
            println("MainActivity: Exception calling Flutter method: ${e.message}")
        }
        
        println("MainActivity: navigationCancelled event sent")
    }

    fun sendNavigationEventPublic(eventType: String, data: Map<String, Any>) {
        sendNavigationEvent(eventType, data)
    }

    private fun sendNavigationEvent(eventType: String, data: Map<String, Any>) {
        println("MainActivity: sendNavigationEvent called with type: $eventType")
        val eventData = mapOf(
            "event" to eventType,
            "data" to data
        )
        println("MainActivity: eventSink is null? ${eventSink == null}")
        eventSink?.success(eventData)
        println("MainActivity: event sent to Flutter")
    }

    override fun onDestroy() {
        super.onDestroy()
        methodChannel?.setMethodCallHandler(null)
        eventChannel?.setStreamHandler(null)
    }
}
