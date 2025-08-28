import UIKit
import MapboxNavigation
import MapboxCoreNavigation
import MapboxDirections
import Flutter
import CoreLocation
import MapboxMaps

class NavigationViewController: UIViewController {
    private var eventSink: FlutterEventSink?
    private var eventChannel: FlutterEventChannel?
    
    private var destinationLat: Double = 0.0
    private var destinationLng: Double = 0.0
    private var arrivalRadius: Double = 20.0
    private var shouldSimulate: Bool = false
    private var language: String = "en"
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupNavigation()
    }
    
    func configure(
        destinationLat: Double,
        destinationLng: Double,
        arrivalRadius: Double,
        shouldSimulate: Bool,
        language: String,
        eventChannel: FlutterEventChannel
    ) {
        self.destinationLat = destinationLat
        self.destinationLng = destinationLng
        self.arrivalRadius = arrivalRadius
        self.shouldSimulate = shouldSimulate
        self.language = language
        self.eventChannel = eventChannel
        
        eventChannel.setStreamHandler(self)
    }
    
    private func setupNavigation() {
        // Check location permissions
        guard CLLocationManager.locationServicesEnabled() else {
            sendEvent(event: "NavigationEventType.navigationCancelled", data: ["reason": "Location services disabled"])
            return
        }
        
        let locationManager = CLLocationManager()
        locationManager.requestWhenInUseAuthorization()
        
        // Create destination coordinate
        let destinationCoordinate = CLLocationCoordinate2D(latitude: destinationLat, longitude: destinationLng)
        
        // Start navigation using Drop-in Navigation
        startDropInNavigation(to: destinationCoordinate)
    }
    
    private func startDropInNavigation(to destinationCoordinate: CLLocationCoordinate2D) {
        // Create NavigationOptions for customization
        var navigationOptions = NavigationOptions()
        
        // Enable simulation if requested
        if shouldSimulate {
            navigationOptions.simulationMode = .always
        }
        
        // Create navigation view controller with drop-in UI
        let navigationViewController = NavigationViewController(
            for: [],
            navigationOptions: navigationOptions
        )
        
        // Set up delegate to listen for navigation events
        navigationViewController.delegate = self
        
        // Present the navigation UI
        present(navigationViewController, animated: true) {
            // Send initial events
            self.sendEvent(event: "NavigationEventType.routeBuilt", data: [:])
            self.sendEvent(event: "NavigationEventType.navigationRunning", data: [:])
            
            // Start navigation to destination
            navigationViewController.navigationService.requestRoutes(
                to: Waypoint(coordinate: destinationCoordinate),
                completionHandler: { [weak self] result in
                    switch result {
                    case .success(let response):
                        // Navigation will start automatically with drop-in UI
                        break
                    case .failure(let error):
                        self?.sendEvent(event: "NavigationEventType.navigationCancelled", 
                                       data: ["reason": error.localizedDescription])
                    }
                }
            )
        }
        
        // Set up route progress observer
        setupNavigationObservers(navigationViewController)
    }
    
    private func setupNavigationObservers(_ navigationVC: NavigationViewController) {
        // Observe route progress
        navigationVC.navigationService.addRouteProgressObserver(self)
    }
    
    private func sendEvent(event: String, data: [String: Any]) {
        let eventData = ["event": event, "data": data] as [String: Any]
        eventSink?(eventData)
    }
}

// MARK: - NavigationViewControllerDelegate
extension NavigationViewController: NavigationViewControllerDelegate {
    func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
        if canceled {
            sendEvent(event: "NavigationEventType.navigationCancelled", data: [:])
        } else {
            sendEvent(event: "NavigationEventType.navigationFinished", data: [:])
        }
        
        // Dismiss the navigation view controller and return to Flutter
        navigationViewController.dismiss(animated: true, completion: nil)
        self.dismiss(animated: true, completion: nil)
    }
}

// MARK: - RouteProgressObserver
extension NavigationViewController: RouteProgressObserver {
    func routeProgressDidChange(_ routeProgress: RouteProgress) {
        let distanceRemaining = routeProgress.distanceRemaining
        let durationRemaining = routeProgress.durationRemaining
        
        sendEvent(event: "NavigationEventType.progressChange", data: [
            "distanceRemaining": distanceRemaining,
            "durationRemaining": durationRemaining
        ])
    }
}

// MARK: - FlutterStreamHandler
extension NavigationViewController: FlutterStreamHandler {
    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = events
        return nil
    }
    
    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        self.eventSink = nil
        return nil
    }
}