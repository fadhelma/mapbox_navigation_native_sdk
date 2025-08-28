_# Mapbox Navigation SDK

A Flutter package that provides native Mapbox Navigation SDK integration for Android and iOS. This package offers turn-by-turn navigation with route preview, progress tracking, and event-driven architecture.

## Features

✅ **Native Navigation UI**: Full-screen navigation experience using Mapbox Navigation SDK  
✅ **Turn-by-Turn Guidance**: Voice instructions and visual guidance  
✅ **Route Preview**: Shows full route overview before starting navigation  
✅ **Real-time Progress**: Distance remaining, duration, and ETA updates  
✅ **Event-Driven**: Listen to navigation events (start, progress, completion, cancellation)  
✅ **No Exit Dialogs**: Seamless cancellation flow that returns to Flutter  
✅ **Cross Platform**: Supports both Android and iOS  

## Prerequisites

### 1. Mapbox Account
Create a free account at [mapbox.com](https://www.mapbox.com/) and obtain your access tokens.

### 2. Android Setup

#### Add Mapbox Maven Repository
Add the following to your `android/build.gradle` (project level):

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url 'https://api.mapbox.com/downloads/v2/releases/maven'
            authentication {
                basic(BasicAuthentication)
            }
            credentials {
                // Provide your secret token here
                username = "mapbox"
                password = "YOUR_SECRET_MAPBOX_TOKEN"
            }
        }
    }
}
```

#### Configure Permissions
Add location permissions to `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

#### Update Navigation Activity Layout
Create `android/app/src/main/res/layout/activity_navigation.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.mapbox.navigation.dropin.NavigationView
        android:id="@+id/navigationView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</RelativeLayout>
```

### 3. iOS Setup

#### Configure Info.plist
Add location permissions to `ios/Runner/Info.plist`:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs location access for navigation.</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>This app needs location access for navigation.</string>
```

## Installation

### Option 1: From GitHub (Recommended)

Add this to your `pubspec.yaml`:

```yaml
dependencies:
  mapbox_navigation_sdk:
    git:
      url: https://github.com/yourusername/mapbox_navigation_sdk.git
      ref: main
```

### Option 2: Local Path (for development)

```yaml
dependencies:
  mapbox_navigation_sdk:
    path: ../path/to/mapbox_navigation_sdk
```

Then run:
```bash
flutter pub get
```

## Usage

### 1. Import the Package

```dart
import 'package:mapbox_navigation_sdk/mapbox_navigation_sdk.dart';
```

### 2. Setup Navigation Listener

```dart
class MyNavigationPage extends StatefulWidget {
  @override
  _MyNavigationPageState createState() => _MyNavigationPageState();
}

class _MyNavigationPageState extends State<MyNavigationPage> {
  StreamSubscription<NavigationEvent>? _navigationSubscription;

  @override
  void initState() {
    super.initState();
    MapboxNavigation.setupMethodCallHandler();
    _setupNavigationListener();
  }

  void _setupNavigationListener() {
    _navigationSubscription = MapboxNavigation.onNavigationEvent.listen((event) {
      switch (event.type) {
        case NavigationEventType.routeBuilt:
          print('Route built successfully');
          break;
        case NavigationEventType.navigationRunning:
          print('Navigation started');
          break;
        case NavigationEventType.progressChange:
          if (event.progress != null) {
            final distanceKm = (event.progress!.distanceRemaining / 1000).toStringAsFixed(1);
            final timeMin = (event.progress!.durationRemaining / 60).toStringAsFixed(1);
            print('Progress: ${distanceKm}km remaining, ${timeMin} minutes ETA');
          }
          break;
        case NavigationEventType.navigationFinished:
          print('Navigation completed');
          break;
        case NavigationEventType.navigationCancelled:
          print('Navigation cancelled');
          // Handle cancellation - user returned to your app
          _handleNavigationCancelled();
          break;
        case NavigationEventType.navigationArrive:
          print('Arrived at destination');
          break;
      }
    });
  }

  void _handleNavigationCancelled() {
    // User clicked X or back button in native navigation
    // You can show a map with route, return to previous screen, etc.
    Navigator.pop(context);
  }

  @override
  void dispose() {
    _navigationSubscription?.cancel();
    super.dispose();
  }
}
```

### 3. Start Navigation

```dart
Future<void> _startNavigation() async {
  // Request location permission first
  final permission = await Permission.locationWhenInUse.request();
  if (permission != PermissionStatus.granted) {
    // Handle permission denied
    return;
  }

  final bool success = await MapboxNavigation.startNavigation(
    destinationLatitude: 37.7749,    // San Francisco
    destinationLongitude: -122.4194,
    arrivalRadius: 50.0,             // Optional: arrival radius in meters
    shouldSimulateRoute: false,      // Optional: simulate route for testing
    language: 'en',                  // Optional: navigation language
  );

  if (!success) {
    print('Failed to start navigation');
  }
}
```

### 4. Handle Navigation Events

The package provides the following event types:

- **`NavigationEventType.routeBuilt`**: Route calculation completed
- **`NavigationEventType.navigationRunning`**: Navigation started with turn-by-turn guidance
- **`NavigationEventType.progressChange`**: Real-time progress updates (distance, duration)
- **`NavigationEventType.navigationFinished`**: User completed the route
- **`NavigationEventType.navigationCancelled`**: User cancelled navigation (clicked X/back)
- **`NavigationEventType.navigationArrive`**: User arrived at destination

## Complete Example

```dart
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:mapbox_navigation_sdk/mapbox_navigation_sdk.dart';
import 'dart:async';

class NavigationScreen extends StatefulWidget {
  @override
  _NavigationScreenState createState() => _NavigationScreenState();
}

class _NavigationScreenState extends State<NavigationScreen> {
  StreamSubscription<NavigationEvent>? _navigationSubscription;

  @override
  void initState() {
    super.initState();
    MapboxNavigation.setupMethodCallHandler();
    _setupNavigationListener();
  }

  void _setupNavigationListener() {
    _navigationSubscription = MapboxNavigation.onNavigationEvent.listen((event) {
      switch (event.type) {
        case NavigationEventType.routeBuilt:
          _showSnackBar('Route built successfully');
          break;
        case NavigationEventType.navigationRunning:
          _showSnackBar('Navigation started');
          break;
        case NavigationEventType.progressChange:
          // Handle progress updates
          break;
        case NavigationEventType.navigationFinished:
          _showSnackBar('Navigation completed');
          break;
        case NavigationEventType.navigationCancelled:
          _showSnackBar('Navigation cancelled');
          break;
        case NavigationEventType.navigationArrive:
          _showSnackBar('Arrived at destination');
          break;
      }
    });
  }

  Future<void> _startNavigation() async {
    // Request permission
    final permission = await Permission.locationWhenInUse.request();
    if (permission != PermissionStatus.granted) {
      _showSnackBar('Location permission required');
      return;
    }

    // Start navigation
    final success = await MapboxNavigation.startNavigation(
      destinationLatitude: 24.820472,  // Riyadh
      destinationLongitude: 46.735904,
      arrivalRadius: 50.0,
      shouldSimulateRoute: false,
      language: 'en',
    );

    if (!success) {
      _showSnackBar('Failed to start navigation');
    }
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Navigation')),
      body: Center(
        child: ElevatedButton(
          onPressed: _startNavigation,
          child: Text('Start Navigation'),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _navigationSubscription?.cancel();
    super.dispose();
  }
}
```

## API Reference

### MapboxNavigation Class

#### Methods

**`setupMethodCallHandler()`**
Sets up method call handler for receiving native callbacks. Call this in `initState()`.

**`startNavigation({required double destinationLatitude, required double destinationLongitude, double arrivalRadius = 50.0, bool shouldSimulateRoute = false, String language = 'en'}) → Future<bool>`**
Starts navigation to the specified destination.

Parameters:
- `destinationLatitude`: Destination latitude coordinate
- `destinationLongitude`: Destination longitude coordinate  
- `arrivalRadius`: Arrival radius in meters (default: 50.0)
- `shouldSimulateRoute`: Enable route simulation for testing (default: false)
- `language`: Navigation language code (default: 'en')

Returns: `true` if navigation started successfully, `false` otherwise.

**`cancelNavigation() → Future<void>`**
Programmatically cancels active navigation.

#### Events

**`onNavigationEvent → Stream<NavigationEvent>`**
Stream of navigation events. Listen to this stream to receive real-time updates.

### NavigationEvent Class

**Properties:**
- `type`: NavigationEventType enum
- `data`: Map<String, dynamic> containing event-specific data
- `progress`: NavigationProgress? (available for progressChange events)

### NavigationProgress Class

**Properties:**
- `distanceRemaining`: double - Remaining distance in meters
- `durationRemaining`: double - Remaining time in seconds

## Navigation Flow

1. **Route Building**: When `startNavigation()` is called, the package calculates the route
2. **Route Preview**: Shows full route overview on map before starting turn-by-turn guidance
3. **Active Navigation**: User gets turn-by-turn directions with voice guidance
4. **Progress Updates**: Real-time distance/duration updates via events
5. **Completion**: Navigation ends when user arrives or cancels
6. **Return to Flutter**: User returns to your Flutter app (no exit dialogs)

## Troubleshooting

### Android Issues

**Build Error: "Could not resolve mapbox dependencies"**
- Ensure you've added the Mapbox Maven repository with correct credentials
- Verify your secret token has download permissions

**Navigation Activity not found**
- Make sure `activity_navigation.xml` exists in `res/layout/`
- Verify the NavigationView is properly configured

### iOS Issues

**Location permission denied**
- Check Info.plist has location usage descriptions
- Ensure you're requesting permissions before starting navigation

### General Issues

**No navigation events received**
- Call `MapboxNavigation.setupMethodCallHandler()` in `initState()`
- Make sure you're listening to `MapboxNavigation.onNavigationEvent`

**Navigation starts but immediately crashes**
- Check device/simulator has location services enabled
- Verify Mapbox access token is valid and has navigation permissions

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues, feature requests, or questions:
- Open an issue on [GitHub](https://github.com/yourusername/mapbox_navigation_sdk/issues)
- Check existing issues for similar problems
- Provide detailed information about your setup and the problem_