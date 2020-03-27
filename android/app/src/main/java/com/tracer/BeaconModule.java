package com.tracer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import org.altbeacon.beacon.BeaconTransmitter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BeaconModule extends ReactContextBaseJavaModule {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    //constructor
    public BeaconModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    //Mandatory function getName that specifies the module name
    @Override
    public String getName() {
        return "Beacon";
    }


    @ReactMethod
    public void stopBroadcast() {
        Log.i("BLE","stopping broadcast");
        Intent serviceIntent = new Intent(getReactApplicationContext(), BeaconTransmitterService.class);

        getReactApplicationContext().stopService(serviceIntent);


    }

    @ReactMethod
    public void stopScanning() {
        BluetoothLeScanner mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        mBluetoothLeScanner.stopScan(mScanCallback);

    }

    @ReactMethod
    public void startBroadcast(final String uuid) {

        try {
            Log.i("BLE ",uuid);
            ParcelUuid pUuid = new ParcelUuid(UUID.fromString(uuid));

            int result = BeaconTransmitter.checkTransmissionSupported(getReactApplicationContext());
            if(result == BeaconTransmitter.SUPPORTED) {
                Intent serviceIntent = new Intent(getReactApplicationContext(), BeaconTransmitterService.class);
                serviceIntent.putExtra("uuid", uuid);
                ContextCompat.startForegroundService(getReactApplicationContext(), serviceIntent);

            }

        } catch (Exception e) {
            Log.i("BLE",e.getMessage());
            e.printStackTrace();
        }
    }


        @ReactMethod
    public void startScanning(final String uuid) {



        BluetoothLeScanner mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
//        ScanFilter filter = new ScanFilter.Builder()
//                .setServiceUuid(new ParcelUuid(UUID.fromString(uuid)))
//                .build();


        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        Log.i("BLE", "starting scanning");


        mBluetoothLeScanner.startScan(mScanCallback);

    }



    AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i("BLE", "Advertising onStartSuccess " + settingsInEffect);
            ReactContext currentContext = getReactApplicationContext();

            WritableMap params = Arguments.createMap();
            params.putInt("txPowerLevel", settingsInEffect.getTxPowerLevel());
            params.putInt("mode", settingsInEffect.getMode());

            currentContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onBroadcastSuccess", params);

            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e("BLE", "Advertising onStartFailure: " + errorCode);

            WritableMap params = Arguments.createMap();
            params.putInt("error", errorCode);
            ReactContext currentContext = getReactApplicationContext();

            currentContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onBroadcastFailure", params);

            super.onStartFailure(errorCode);
        }
    };

    ScanCallback mScanCallback = new ScanCallback() {

        WritableMap getReadableMap(ScanResult result) {

            byte[] uuidBytes = Arrays.copyOfRange(result.getScanRecord().getBytes(), 6, 22);

            String uuidWithoutDash = toHexString(uuidBytes);

            String uuid = uuidWithoutDash.substring(0,8)+"-"+uuidWithoutDash.substring(8,12)+"-"+uuidWithoutDash.substring(12,16)+"-"+uuidWithoutDash.substring(16,20)+"-"+uuidWithoutDash.substring(20);
//            String deviceId = result.getScanRecord().getServiceUuids().get(0).getUuid().toString();

            WritableMap params = Arguments.createMap();
            params.putString("deviceId", uuid);
            params.putInt("rssi", result.getRssi());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int txPower = result.getTxPower();
                params.putInt("txPower", txPower);
            }

            return params;
        }

        public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getDevice() == null){
                return;

            }
//            result.getAdvertisingSid();


//            if(result.getScanRecord().getServiceUuids() != null && result.getScanRecord().getServiceUuids().get(0) != null){

//            Log.i("BLE", "On ScaN Result " +result.getScanRecord().getServiceUuids().get(0) );
//            System.out.println("Power " + result.getRssi()+" : "+ result.getScanRecord().getTxPowerLevel()+": "+result.getTxPower() );

              WritableMap  params = this.getReadableMap(result);


            ReactContext currentContext = getReactApplicationContext();
            currentContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onScanResult", params);

            }

//        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i("BLE", "Batch results "+results);
            WritableArray params = Arguments.createArray();
            for(ScanResult result : results) {
                WritableMap map = this.getReadableMap(result);
                params.pushMap(map);
            }

            ReactContext currentContext = getReactApplicationContext();

            currentContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onScanResultBulk", params);


        }

        @Override
        public void onScanFailed(int errorCode) {

            WritableMap params = Arguments.createMap();
            params.putInt("error", errorCode);
            ReactContext currentContext = getReactApplicationContext();

            currentContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onScanFailed", params);

            Log.e("BLE", "Discovery onScanFailed: " + errorCode);
        }
    };

    static String toHexString(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int c = bytes[i] & 0xFF;
            chars[i * 2] = HEX[c >>> 4];
            chars[i * 2 + 1] = HEX[c & 0x0F];
        }
        return new String(chars).toLowerCase();
    }

}