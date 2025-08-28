package mapbox.navigation.sdk.mapbox_navigation_sdk

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.EventChannel

class MapboxNavigationSdkPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var methodChannel: MethodChannel
  private lateinit var eventChannel: EventChannel
  private var context: Context? = null
  private var activityBinding: ActivityPluginBinding? = null
  private var eventSink: EventChannel.EventSink? = null

  companion object {
    var pluginInstance: MapboxNavigationSdkPlugin? = null
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "mapbox_navigation")
    methodChannel.setMethodCallHandler(this)
    
    eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "mapbox_navigation_events")
    eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
      }

      override fun onCancel(arguments: Any?) {
        eventSink = null
      }
    })
    
    context = flutterPluginBinding.applicationContext
    pluginInstance = this
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
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
        sendNavigationEvent("NavigationEventType.navigationCancelled", emptyMap<String, Any>())
        result.success(null)
      }
      "navigationCancelled" -> {
        println("Plugin: navigationCancelled method channel call received")
        result.success("done closed")
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  private fun startMapboxNavigation(
    destinationLat: Double,
    destinationLng: Double,
    arrivalRadius: Double,
    shouldSimulate: Boolean,
    language: String
  ) {
    val activity = activityBinding?.activity ?: return
    
    val intent = Intent(activity, NavigationActivity::class.java).apply {
      putExtra("destinationLatitude", destinationLat)
      putExtra("destinationLongitude", destinationLng)
      putExtra("arrivalRadius", arrivalRadius)
      putExtra("shouldSimulateRoute", shouldSimulate)
      putExtra("language", language)
    }
    activity.startActivity(intent)
  }

  fun sendNavigationEvent(eventType: String, data: Map<String, Any>) {
    println("Plugin: sendNavigationEvent called with type: $eventType")
    val eventData = mapOf(
      "event" to eventType,
      "data" to data
    )
    
    Handler(Looper.getMainLooper()).post {
      eventSink?.success(eventData)
      println("Plugin: event sent to Flutter")
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel.setMethodCallHandler(null)
    eventChannel.setStreamHandler(null)
    pluginInstance = null
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activityBinding = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activityBinding = binding
  }

  override fun onDetachedFromActivity() {
    activityBinding = null
  }
}