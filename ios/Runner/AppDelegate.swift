import UIKit
import Flutter
import MapboxNavigation

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    GeneratedPluginRegistrant.register(with: self)
    
    let controller: FlutterViewController = window?.rootViewController as! FlutterViewController
    let navigationChannel = FlutterMethodChannel(name: "mapbox_navigation", binaryMessenger: controller.binaryMessenger)
    let eventChannel = FlutterEventChannel(name: "mapbox_navigation_events", binaryMessenger: controller.binaryMessenger)
    
    navigationChannel.setMethodCallHandler({ (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
      guard call.method == "startNavigation" else {
        result(FlutterMethodNotImplemented)
        return
      }
      
      guard let args = call.arguments as? [String: Any],
            let destinationLat = args["destinationLatitude"] as? Double,
            let destinationLng = args["destinationLongitude"] as? Double else {
        result(FlutterError(code: "INVALID_ARGUMENTS", message: "Destination coordinates required", details: nil))
        return
      }
      
      let arrivalRadius = args["arrivalRadius"] as? Double ?? 20.0
      let shouldSimulate = args["shouldSimulateRoute"] as? Bool ?? false
      let language = args["language"] as? String ?? "en"
      
      self.startMapboxNavigation(
        controller: controller,
        destinationLat: destinationLat,
        destinationLng: destinationLng,
        arrivalRadius: arrivalRadius,
        shouldSimulate: shouldSimulate,
        language: language,
        eventChannel: eventChannel
      )
      
      result(true)
    })
    
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
  
  private func startMapboxNavigation(
    controller: FlutterViewController,
    destinationLat: Double,
    destinationLng: Double,
    arrivalRadius: Double,
    shouldSimulate: Bool,
    language: String,
    eventChannel: FlutterEventChannel
  ) {
    DispatchQueue.main.async {
      let navigationViewController = NavigationViewController()
      navigationViewController.configure(
        destinationLat: destinationLat,
        destinationLng: destinationLng,
        arrivalRadius: arrivalRadius,
        shouldSimulate: shouldSimulate,
        language: language,
        eventChannel: eventChannel
      )
      controller.present(navigationViewController, animated: true, completion: nil)
    }
  }
}
