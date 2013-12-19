package com.dennis.vitalsigns;

import java.util.List;
import java.util.UUID;

import android.bluetooth.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;


public class BlueToothMethods {

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	private boolean isHeartRateMonitor=false;
	private Context mContext;

	public BlueToothMethods(Context mContext) {
		this.mContext = mContext;
	}

	public boolean isHeartRateDeviceSet(){		
		if(getHeartRateDeviceAddress().equals("")){
			return false;
		}
		return true;
	}

	public void setHeartRateDevice(String deviceName, String deviceAddress){
		try {
			SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(mContext);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("key_device_heart_rate_name", deviceName);
			editor.putString("key_device_heart_rate_address", deviceAddress);
			editor.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getHeartRateDeviceAddress(){
		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(mContext);
		String deviceAddress=settings.getString("key_device_heart_rate_address","");
		return deviceAddress;
	}

	public String getHeartRateDeviceName(){
		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(mContext);
		String deviceAddress=settings.getString("key_device_heart_rate_name","");
		return deviceAddress;
	}

/**
 * The rational for having this method is because a certain device which may have a heart rate service may not
 * have that service at a latter date/time. may be other services on the device are still working but not the
 * heart rate service
 * 
 * @param deviceAddress
 * @return
 */
	public boolean isDeviceHeartRateMonitor(String deviceAddress) {
		
		if(isBlueToothLeSupported()){
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
			if (device == null) {
				CommonMethods.Log("Device not found.  Unable to connect.");
				return false;
			}

			mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);// this step goes off into it's own thread.

			for(int i=0;i<Preferences.deviceScanTime;i++){				
				//break from loop if a heart rate monitor is found
				if(isHeartRateMonitor){
					disconnectAndClose();
					break;
				} 

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			disconnectAndClose();// disconnect and close irrespective of the results.		 
			return isHeartRateMonitor;	 

		}else{
			return false;
		}

	}

	public boolean isBlueToothLeSupported(){
		CommonMethods.Log("in isBlueToothLeSupported()");
		// Use this check to determine whether BLE is supported on the device.  Then you can
		// selectively disable BLE-related features.
		if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			return false;
		}

		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
		// BluetoothAdapter through BluetoothManager.

		final BluetoothManager bluetoothManager =
				(BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			CommonMethods.Log("Error bluetooth not supported");
			return false;
		}

		return true;
	}
	

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				CommonMethods.Log( "Connected to GATT server.");
				CommonMethods.Log( "Attempting to start service discovery:");
				mBluetoothGatt.discoverServices();

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				CommonMethods.Log( "Disconnected from GATT server.");
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				CommonMethods.Log( "in onServicesDiscovered");
				isHeartRateMonitor=doesHeartRateCharacteristicExists();
			} else {
				CommonMethods.Log( "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic,
				int status) {
			CommonMethods.Log( "in onCharacteristicRead");
			if (status == BluetoothGatt.GATT_SUCCESS) {
				CommonMethods.Log( "Characteristic sucessfully read");
			}
		}


	};


	private boolean  doesHeartRateCharacteristicExists() {
		List<BluetoothGattService> gattServices =mBluetoothGatt.getServices();

		for (BluetoothGattService gattService : gattServices) {
			List<BluetoothGattCharacteristic> gattCharacteristics =	gattService.getCharacteristics();

			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {   	                    	                
				final int charaProp = gattCharacteristic.getProperties();
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					if (mBluetoothAdapter != null || mBluetoothGatt != null) { 	                	
						if (UUID.fromString(GattAttributes.HEART_RATE_MEASUREMENT).equals(gattCharacteristic.getUuid())) {

							return true;
						}
					}
				}

			}
		}
		return false;
	}


	private void disconnectAndClose(){

		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			CommonMethods.Log( "In disconnectAndClose(). BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
		mBluetoothGatt.close();
		mBluetoothGatt = null;

	}




}
