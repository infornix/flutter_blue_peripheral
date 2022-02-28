
import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/services.dart';

class Frccblue {
  static const MethodChannel _channel = const MethodChannel('frccblue');

  static Future<dynamic> init({
    required Function didReceiveRead,
    required Function didReceiveWrite,
    required Function didSubscribeTo,
    required Function didUnsubscribeFrom,
    required Function peripheralManagerDidUpdateState,
  }) async {
    _channel.setMethodCallHandler((MethodCall call) {
      if (call.method == 'didReceiveRead') {
        return Future(() {
          return didReceiveRead(call);
        });
      }
      if (call.method == 'didReceiveWrite') {
        return Future(() {
          return didReceiveWrite(call);
        });
      }
      if (call.method == 'didSubscribeTo') {
        return Future(() {
          return didSubscribeTo(call);
        });
      }
      if (call.method == 'didUnsubscribeFrom') {
        return Future(() {
          return didUnsubscribeFrom(call);
        });
      }
      if (call.method == 'peripheralManagerDidUpdateState') {
        return Future(() {
          return peripheralManagerDidUpdateState(call);
        });
      }
      return Future(() {});
    });
  }

  static Future<void> peripheralUpdateValue({
    required String centralUuid,
    required String characteristicUuid,
    required Uint8List data,
  }) async {
    await _channel.invokeMethod('peripheralUpdateValue', {
      'centralUuidString': centralUuid,
      'characteristicUuidString': characteristicUuid,
      'data': data,
    });
  }

  static Future<void> startPeripheral({
    required String serviceUuid,
    required String characteristicUuid,
  }) async {
    await _channel.invokeMethod('startPeripheral', {
      'serviceUuid': serviceUuid,
      'characteristicUuid': characteristicUuid,
    });
  }

  static void stopPeripheral(){
    _channel.invokeMethod('stopPeripheral');
  }

  static Future<String?> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
