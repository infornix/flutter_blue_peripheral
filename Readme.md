
# flutter_blue_peripheral

Flutter Bluetooth LE (BLE) Peripheral Plugin.

## Supported platforms

iOS / Android

## How to use

```
dependencies:
  flutter_blue_peripheral:
    git:
      url: https://github.com/tsukumijima/flutter_blue_peripheral
```

```
import 'package:flutter_blue_peripheral/flutter_blue_peripheral.dart';

// initialize
FlutterBluePeripheral.init(didReceiveRead:(MethodCall call){
      print(call.arguments);
      return Uint8List.fromList([11,2,3,4,5,6,7,8,9,10,]);
    }, didReceiveWrite:(MethodCall call){
      print(call.arguments);
    },didSubscribeTo: (MethodCall call){
      print(call.arguments);
//      FlutterBluePeripheral.peripheralUpdateValue()
    },didUnsubscribeFrom: (MethodCall call){
      print(call.arguments);
    },peripheralManagerDidUpdateState: (MethodCall call){
      print(call.arguments);
    });


// start peripheral server
FlutterBluePeripheral.startPeripheral("00000000-0000-0000-0000-AAAAAAAAAAA1", "00000000-0000-0000-0000-AAAAAAAAAAA2").then((_){});

// stop peripheral server
FlutterBluePeripheral.stopPeripheral();
```

## More

### peripheralManagerDidUpdateState

iOS UpdateState:

```
switch peripheral.state {
    case .unknown:
        print("Unknown")
        state = "unknown"
    case .resetting:
        print("Resetting")
        state = "resetting"
    case .unsupported:
        print("Unsupported")
        state = "unsupported"
    case .unauthorized:
        print("Unauthorized")
        state = "unauthorized"
    case .poweredOff:
        print("Not Started")
        state = "poweredOff"
        self.peripheralManager?.stopAdvertising()
    case .poweredOn:
        print("Available")
        state = "poweredOn"
```

Android UpdateState：

```
"unknown"
"poweredOff"
"poweredOn"
```

iOS does not have device connect and disconnect callbacks, but Android does. Therefore, the central device should subscribe to the Characteristic.  
Then didSubscribeTo means the device is connected and didUnsubscribeFrom means the device is disconnected. didUnsubscribeFrom on the Android side he is triggered twice if the device actively cancels the subscription.
