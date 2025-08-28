import 'dart:async';
import 'package:flutter/services.dart';

enum NavigationEventType {
  routeBuilt,
  navigationRunning,
  progressChange,
  navigationFinished,
  navigationCancelled,
  navigationArrive,
}

class NavigationProgress {
  final double distanceRemaining;
  final double durationRemaining;

  NavigationProgress({
    required this.distanceRemaining,
    required this.durationRemaining,
  });

  factory NavigationProgress.fromMap(Map<String, dynamic> map) {
    return NavigationProgress(
      distanceRemaining: map['distanceRemaining']?.toDouble() ?? 0.0,
      durationRemaining: map['durationRemaining']?.toDouble() ?? 0.0,
    );
  }
}

class NavigationEvent {
  final NavigationEventType type;
  final Map<String, dynamic> data;

  NavigationEvent({
    required this.type,
    required this.data,
  });

  NavigationProgress? get progress => 
    type == NavigationEventType.progressChange 
      ? NavigationProgress.fromMap(data) 
      : null;
}

class MapboxNavigation {
  static const MethodChannel _methodChannel = MethodChannel('mapbox_navigation');
  static const EventChannel _eventChannel = EventChannel('mapbox_navigation_events');

  static Stream<NavigationEvent>? _eventStream;

  static void setupMethodCallHandler() {
    _methodChannel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'navigationCancelled':
          print('Flutter: Received navigationCancelled method call');
          print('done closed');
          break;
        default:
          print('Flutter: Unknown method call: ${call.method}');
      }
    });
  }

  static Stream<NavigationEvent> get onNavigationEvent {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map((event) {
      try {
        // More robust type handling
        final eventMap = event as Map<Object?, Object?>;
        final eventTypeString = eventMap['event']?.toString() ?? '';
        final dataMap = eventMap['data'] as Map<Object?, Object?>? ?? <Object?, Object?>{};
        
        // Convert to proper types
        final Map<String, dynamic> data = <String, dynamic>{};
        dataMap.forEach((key, value) {
          if (key != null) {
            data[key.toString()] = value;
          }
        });
        
        NavigationEventType eventType;
        switch (eventTypeString) {
          case 'NavigationEventType.routeBuilt':
            eventType = NavigationEventType.routeBuilt;
            break;
          case 'NavigationEventType.navigationRunning':
            eventType = NavigationEventType.navigationRunning;
            break;
          case 'NavigationEventType.progressChange':
            eventType = NavigationEventType.progressChange;
            break;
          case 'NavigationEventType.navigationFinished':
            eventType = NavigationEventType.navigationFinished;
            break;
          case 'NavigationEventType.navigationCancelled':
            eventType = NavigationEventType.navigationCancelled;
            break;
          case 'NavigationEventType.navigationArrive':
            eventType = NavigationEventType.navigationArrive;
            break;
          default:
            eventType = NavigationEventType.navigationCancelled;
        }
        
        return NavigationEvent(type: eventType, data: data);
      } catch (e) {
        print('Error parsing navigation event: $e');
        return NavigationEvent(
          type: NavigationEventType.navigationCancelled, 
          data: {'error': e.toString()}
        );
      }
    });
    
    return _eventStream!;
  }

  static Future<bool> startNavigation({
    required double destinationLatitude,
    required double destinationLongitude,
    double arrivalRadius = 50.0,
    bool shouldSimulateRoute = false,
    String language = 'en',
  }) async {
    try {
      final result = await _methodChannel.invokeMethod('startNavigation', {
        'destinationLatitude': destinationLatitude,
        'destinationLongitude': destinationLongitude,
        'arrivalRadius': arrivalRadius,
        'shouldSimulateRoute': shouldSimulateRoute,
        'language': language,
      });
      
      return result as bool;
    } on PlatformException catch (e) {
      print('Error starting navigation: ${e.message}');
      return false;
    }
  }

  static Future<void> cancelNavigation() async {
    try {
      await _methodChannel.invokeMethod('cancelNavigation');
    } on PlatformException catch (e) {
      print('Error cancelling navigation: ${e.message}');
    }
  }
}