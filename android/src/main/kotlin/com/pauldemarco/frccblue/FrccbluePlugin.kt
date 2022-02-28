package com.pauldemarco.frccblue

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.*
import kotlin.collections.HashMap


class FrccbluePlugin() : MethodCallHandler {

    private var TAG = "FrccbluePlugin";

    companion object {
        var activity: Activity? = null
        var channel: MethodChannel? = null
        var registerReceiver: Boolean = false

        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            var channel = MethodChannel(registrar.messenger(), "frccblue")
            channel.setMethodCallHandler(FrccbluePlugin())
            FrccbluePlugin.activity = registrar.activity()
            FrccbluePlugin.channel = channel
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        }
        if (call.method.equals("startPeripheral")) {
            Log.i(this., "startPeripheral")
            Service_UUID = call.argument<String>("serviceUuid").toString()
            Characteristic_UUID = call.argument<String>("characteristicUuid").toString()
            startPeripheral()
        }
        if (call.method.equals("stopPeripheral")) {
            Log.i(TAG, "stopPeripheral")
            stopPeripheral()
        }
        if (call.method.equals("peripheralUpdateValue")) {
            var centralUuid = call.argument<String>("centralUuid")
            var characteristicUuid = call.argument<String>("characteristicUuid")
            var data = call.argument<ByteArray>("data")

            val device = centralsDic.get(centralUuid)
            // val characteristic = characteristicsDic.get(characteristicUuid)
            // characteristic?.setValue(data)
            mCharacteristic?.value = data
            mGattServer?.notifyCharacteristicChanged(device, mCharacteristic, false)
        }
    }

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mGattServer: BluetoothGattServer? = null
    private var Service_UUID: String = UUID.randomUUID().toString()
    private var Characteristic_UUID: String = UUID.randomUUID().toString()
    private var centralsDic: MutableMap<String, BluetoothDevice> = HashMap()
    private var characteristicsDic: MutableMap<String, BluetoothGattCharacteristic> = HashMap()
    private var descriptorsDic: MutableMap<String, BluetoothGattDescriptor> = HashMap()
    private val handler: Handler = Handler(Looper.getMainLooper())

    private var mAdvData: AdvertiseData? = null
    private var mAdvScanResponse: AdvertiseData? = null
    private var mAdvSettings: AdvertiseSettings? = null
    private var mBluetoothGattService: BluetoothGattService? = null
    private var mAdvertiser: BluetoothLeAdvertiser? = null
    private var mCharacteristic: BluetoothGattCharacteristic? = null

    private val mAdvCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Not broadcasting:$errorCode")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.i(TAG, "Broadcasting")
        }
    }

    open fun startPeripheral() {

        if (FrccbluePlugin.registerReceiver == false) {
            FrccbluePlugin.registerReceiver = true
            val mR = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action

                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                        var statestr = "unknown"
                        when (state) {
                            BluetoothAdapter.STATE_OFF -> statestr = "poweredOff"
                            BluetoothAdapter.STATE_ON -> statestr = "poweredOn"
                        }
                        handler.post(Runnable {
                            channel?.invokeMethod("peripheralManagerDidUpdateState", statestr)
                        })
                        if (statestr == "poweredOn") {
                            this@FrccbluePlugin.startPeripheral()
                        }
                    }
                }
            }
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            FrccbluePlugin.activity?.registerReceiver(mR, filter)
        }

        mBluetoothManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        mBluetoothAdapter = mBluetoothManager?.adapter

        mAdvSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build()
        mAdvData = AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(ParcelUuid(UUID.fromString(Service_UUID)))
                .build()
        mAdvScanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

        mGattServer = mBluetoothManager!!.openGattServer(activity, mGattServerCallback);
        if (mGattServer == null) {
            ensureBleFeaturesAvailable();
            return;
        }

        mBluetoothGattService = BluetoothGattService(
            UUID.fromString(Service_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )

        mCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(Characteristic_UUID),
            //Read write characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        )

        val bluetoothGattDescriptor = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
        )
        mCharacteristic!!.addDescriptor(bluetoothGattDescriptor)

        mBluetoothGattService = BluetoothGattService(UUID.fromString(Service_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY)
        mBluetoothGattService!!.addCharacteristic(mCharacteristic)

        // Add a service for a total of three services (Generic Attribute and Generic Access
        // are present by default).
        mGattServer!!.addService(mBluetoothGattService);

        if (mBluetoothAdapter!!.isMultipleAdvertisementSupported()) {
            mAdvertiser = mBluetoothAdapter!!.getBluetoothLeAdvertiser();
            mAdvertiser!!.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback);
        } else {
            Toast.makeText(FrccbluePlugin.activity?.applicationContext, "MultipleAdvertisement not Supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureBleFeaturesAvailable() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(FrccbluePlugin.activity?.applicationContext, "Not support host.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Bluetooth not supported")
        }
    }

    /*
     * Create the GATT server instance, attaching all services and
     * characteristics that should be exposed
     */
    private fun initServer() {
        val service = BluetoothGattService(UUID.fromString(Service_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val elapsedCharacteristic = BluetoothGattCharacteristic(UUID.fromString(Characteristic_UUID),
                //Read write characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY)

        val bluetoothGattDescriptor = BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)

        elapsedCharacteristic.addDescriptor(bluetoothGattDescriptor)

        service.addCharacteristic(elapsedCharacteristic)

        mGattServer!!.addService(service)
    }

    /*
     * Callback handles all incoming requests from GATT clients.
     * From connections to read/write requests.
     */
    private val mGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                centralsDic.put(device.address, device)
                Log.i(TAG, "onConnectionStateChange STATE_CONNECTED:" + device.address)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                centralsDic.remove(device.address)
                Log.i(TAG, "onConnectionStateChange STATE_DISCONNECTED:" + device.address)
                handler.post(Runnable {
                    channel?.invokeMethod("didUnsubscribeFrom", hashMapOf(
                        "centralUuid" to device?.address!!,
                        "characteristicUuid" to "",
                    ))
                })
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.i(TAG, "onCharacteristicReadRequest:" + characteristic.uuid.toString())

            if (UUID.fromString(Characteristic_UUID) == characteristic.uuid) {

                val cb = object : MethodChannel.Result {
                    override fun success(p0: Any?) {
                        mGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, p0 as ByteArray)
                    }
                    override fun error(p0: String?, p1: String?, p2: Any?) {
                    }
                    override fun notImplemented() {
                    }
                }
                handler.post(Runnable {
                    channel?.invokeMethod("didReceiveRead", hashMapOf(
                        "centralUuid" to device?.address,
                        "characteristicUuid" to characteristic.uuid.toString(),
                    ), cb);
                })
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            Log.i(TAG, "onCharacteristicWriteRequest:" + characteristic.uuid.toString())
            mGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)

            if (UUID.fromString(Characteristic_UUID) == characteristic.uuid) {
                handler.post(Runnable {
                    channel?.invokeMethod("didReceiveWrite", hashMapOf(
                        "centralUuid" to device?.address,
                        "characteristicUuid" to characteristic.uuid.toString(),
                        "data" to value,
                    ))
                })
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?,
        ) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            Log.i(TAG, "onDescriptorReadRequest:" + descriptor?.uuid.toString())
            mGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?,
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            Log.i(TAG, "onDescriptorWriteRequest:" + descriptor?.uuid.toString() + " preparedWrite:" + preparedWrite + " responseNeeded:" + responseNeeded + "value:" + value)

            mGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)

            if (descriptorsDic.containsKey(descriptor?.uuid.toString())) {
                descriptorsDic.remove(descriptor?.uuid.toString())
                characteristicsDic.remove(descriptor?.characteristic?.uuid.toString())
                handler.post(Runnable {
                    channel?.invokeMethod("didUnsubscribeFrom", hashMapOf(
                        "centralUuid" to device?.address!!,
                        "characteristicUuid" to descriptor?.characteristic?.uuid.toString(),
                    ))
                })
            } else {
                descriptorsDic.put(descriptor?.uuid.toString(), descriptor!!)
                characteristicsDic.put(descriptor?.characteristic?.uuid.toString(), descriptor?.characteristic!!)
                handler.post(Runnable {
                    channel?.invokeMethod("didSubscribeTo", hashMapOf(
                        "centralUuid" to device?.address!!,
                        "characteristicUuid" to descriptor?.characteristic?.uuid.toString(),
                    ))
                })
            }
        }
    }

    /*
     * Terminate the advertiser
     */
    private fun stopPeripheral() {
        if (mGattServer != null) {
            mGattServer!!.close()
        }

        // If stopPeripheral() gets called before close() a null
        // pointer exception is raised.
        mAdvertiser!!.stopPeripheral(mAdvCallback)
    }

    /*
     * Callback handles events from the framework describing
     * if we were successful in starting the advertisement requests.
     */
    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "Peripheral Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.i(TAG, "Peripheral Advertise Failed: $errorCode")
        }
    }
}

