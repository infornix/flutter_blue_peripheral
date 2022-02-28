import Flutter
import UIKit
import CoreBluetooth

public class SwiftFrccbluePlugin: NSObject, FlutterPlugin, CBPeripheralManagerDelegate {

    var TAG = "[FrccbluePlugin] "

    var peripheralManager:CBPeripheralManager?
    var c:CBCentralManager?
    var channel:FlutterMethodChannel?

    let centralDic:NSMutableDictionary = [:]
    let characteristicDic:NSMutableDictionary = [:]

    public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name:"frccblue", binaryMessenger: registrar.messenger())
    let instance = SwiftFrccbluePlugin()
        instance.channel = channel
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    private var Service_UUID: String = "00000000-0000-0000-0000-AAAAAAAAAAA1"
    private var Characteristic_UUID: String = "00000000-0000-0000-0000-AAAAAAAAAAA2"

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "getPlatformVersion" {
            result("iOS " + UIDevice.current.systemVersion)
        }
        if call.method == "startPeripheral" {
            print(TAG + "startPeripheral")
            let param = call.arguments as! Dictionary<String,String>
            Service_UUID = param["serviceUuid"]!
            Characteristic_UUID = param["characteristicUuid"]!
            peripheralManager = CBPeripheralManager.init(delegate: self, queue: .main)
        }
        if call.method == "stopPeripheral" {
            print(TAG + "stopPeripheral")
            peripheralManager?.stopAdvertising()
        }
        if call.method == "peripheralUpdateValue" {
            let param = call.arguments as! NSDictionary
            let centralUuid = param["centralUuid"] as! NSString
            let characteristicUuid = param["characteristicUuid"] as! NSString
            let data = param["data"] as! FlutterStandardTypedData
            peripheralManager?.updateValue(
                data.data,
                for: (characteristicDic[0]) as! CBMutableCharacteristic,
                onSubscribedCentrals: [centralDic[centralUuid] as! CBCentral],
            )
        }
    }

    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        var state = "unknown"
        switch peripheral.state {
        case .unknown:
            state = "unknown"
        case .resetting:
            state = "resetting"
        case .unsupported:
            state = "unsupported"
        case .unauthorized:
            state = "unauthorized"
        case .poweredOff:
            state = "poweredOff"
            self.peripheralManager?.stopAdvertising()
        case .poweredOn:
            state = "poweredOn"
            setupServiceAndCharacteristics()
            let deviceName = UIDevice.current.name;
            self.peripheralManager?.startAdvertising(
                [CBAdvertisementDataServiceUUIDsKey: [CBUUID.init(string: Service_UUID)],
                CBAdvertisementDataLocalNameKey: deviceName,
            ])
        }
        print(TAG + "peripheralManagerDidUpdateState:" + state)
        channel?.invokeMethod("peripheralManagerDidUpdateState", arguments: state)
    }

    private func setupServiceAndCharacteristics() {
        let serviceID = CBUUID.init(string: Service_UUID)
        let service = CBMutableService.init(type: serviceID, primary: true)
        let characteristicID = CBUUID.init(string: Characteristic_UUID)
        let characteristic = CBMutableCharacteristic.init(
            type: characteristicID,
            properties: [.read, .write, .notify],
            permissions: [.readable, .writeable],
            value: nil,
        )
        service.characteristics = [characteristic]
        characteristicDic[0] = characteristic
        self.peripheralManager?.add(service)
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didSubscribeTo characteristic: CBCharacteristic) {
        print(TAG + "didSubscribeTo:" + central.identifier.uuidString)
        centralDic[central.identifier.uuidString] = central
        channel?.invokeMethod("didSubscribeTo", arguments: [
            "centralUuid": central.identifier.uuidString,
            "characteristicUuid": characteristic.uuid.uuidString,
        ])
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didUnsubscribeFrom characteristic: CBCharacteristic) {
        print(TAG + "didUnsubscribeFrom:" + central.identifier.uuidString)
        centralDic[central.identifier.uuidString] = nil
        channel?.invokeMethod("didUnsubscribeFrom", arguments: [
            "centralUuid": central.identifier.uuidString,
            "characteristicUuid": characteristic.uuid.uuidString,
        ])
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveRead request: CBATTRequest) {
        channel?.invokeMethod("didReceiveRead", arguments: [
            "centralUuid": request.central.identifier.uuidString,
            "characteristicUuid": request.characteristic.uuid.uuidString,
        ],
        result: { (data) in
            if let da = data as? FlutterStandardTypedData {
                request.value = da.data
            }
            peripheral.respond(to: request, withResult: .success)
        })
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        for request in requests {
            if let data = request.value {
                channel?.invokeMethod("didReceiveWrite", arguments: [
                    "centralUuid": request.central.identifier.uuidString,
                    "characteristicUuid": request.characteristic.uuid.uuidString,
                    "data": FlutterStandardTypedData.init(bytes: data),
                ])
            }
            request.value = nil
            peripheral.respond(to: request, withResult: .success)
        }
    }
}
