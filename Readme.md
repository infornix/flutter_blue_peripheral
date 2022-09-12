
# flutter_blue_peripheral

A new Flutter plugin project.

## Getting Started

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter development, view the
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

## プラットフォーム

iOS / Android

## 利用方法

```
dependencies:
  flutter_blue_peripheral:
    git:
      url: https://github.com/tsukumijima/flutter_blue_peripheral
```

## 移行

```
Frccblue.init(didReceiveRead:(MethodCall call){
      print(call.arguments);
      return Uint8List.fromList([11,2,3,4,5,6,7,8,9,10,]);
    }, didReceiveWrite:(MethodCall call){
      print(call.arguments);
    },didSubscribeTo: (MethodCall call){
      print(call.arguments);
//      Frccblue.peripheralUpdateValue()
    },didUnsubscribeFrom: (MethodCall call){
      print(call.arguments);
    },peripheralManagerDidUpdateState: (MethodCall call){
      print(call.arguments);
    });

Frccblue.startPeripheral("00000000-0000-0000-0000-AAAAAAAAAAA1", "00000000-0000-0000-0000-AAAAAAAAAAA2").then((_){});
```

## more

### peripheralManagerDidUpdateState

iOS の更新ステータス:

```
switch peripheral.state {
        case .unknown:
            print("未知的")
            state = "unknown"
        case .resetting:
            print("重置中")
            state = "resetting"
        case .unsupported:
            print("不支持")
            state = "unsupported"
        case .unauthorized:
            print("未验证")
            state = "unauthorized"
        case .poweredOff:
            print("未启动")
            state = "poweredOff"
            self.peripheralManager?.stopAdvertising()
        case .poweredOn:
            print("可用")
            state = "poweredOn"
```

Android の更新ステータス：

```
"unknown"
"poweredOff"
"poweredOn"
```

iOS にはデバイスの接続と切断のコールバックがありませんが、Android にはあります。したがって、セントラルデバイスは Characteristic をサブスクライブする必要があります。  
次に、didSubscribeTo はデバイスが接続されていることを意味し、didUnsubscribeFrom はデバイスが切断されていることを意味します。Android 側の didUnsubscribeFrom は、デバイスがアクティブにサブスクリプションをキャンセルした場合に 2 回トリガーされます。
